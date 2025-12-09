package com.example.notifi.api.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notifi.api.core.notification.exceptions.SendAtWindowException;
import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.core.template.exceptions.TemplateCodeNotFoundException;
import com.example.notifi.api.core.template.exceptions.TemplateInactiveException;
import com.example.notifi.api.data.entity.ClientEntity;
import com.example.notifi.api.data.repository.NotificationDeliveryAttemptEntity;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.data.repository.NotificationDeliveryAttemptRepository;
import com.example.notifi.api.data.repository.NotificationRepository;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationRequest;
import com.example.notifi.common.model.Channel;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private NotificationPolicy notificationPolicy;
    @Mock private TemplateService templateService;
    @Mock private NotificationTaskPublisher taskPublisher;
    @Mock private NotificationDeliveryAttemptRepository deliveryAttemptRepository;
    @Mock private Clock clock;

    @InjectMocks private NotificationService service;

    private final ClientPrincipal principal =
        new ClientPrincipal(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Client", 10);

    private ClientEntity client() {
        ClientEntity client = new ClientEntity();
        client.setId(principal.clientId());
        client.setName(principal.name());
        client.setRateLimitPerMin(principal.rateLimitPerMin());
        return client;
    }

    @Test
    void create_ShouldCreateNewNotification() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-1"))
            .thenReturn(Optional.empty());
        ClientEntity client = client();
        when(clientRepository.findById(principal.clientId())).thenReturn(Optional.of(client));
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-1");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Subject");

        CreateNotificationResult result = service.create(request, principal);

        assertThat(result.isReplayed()).isFalse();
        NotificationEntity saved = result.getEntity();
        assertThat(saved.getClientId()).isEqualTo(principal.clientId());
        assertThat(saved.getChannel()).isEqualTo(Channel.EMAIL.name());
        assertThat(saved.getTo()).isEqualTo("user@example.com");
        assertThat(saved.getSubject()).isEqualTo("Subject");
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.CREATED);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);

        verify(taskPublisher).publish(saved, client);
    }

    @Test
    void create_ShouldPersistRequestedSendAt_WhenInFuture() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Instant requested = now.plus(1, ChronoUnit.DAYS);
        ClientEntity client = client();

        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-10"))
            .thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(now);
        when(clientRepository.findById(principal.clientId())).thenReturn(Optional.of(client));
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-10");
        request.setChannel(Channel.EMAIL);
        request.setSendAt(requested);
        request.setTo("future@example.com");
        request.setSubject("Future subject");

        NotificationEntity saved = service.create(request, principal).getEntity();

        assertThat(saved.getSendAt()).isEqualTo(requested);
        assertThat(saved.getSendAt()).isAfter(saved.getCreatedAt());
        verify(taskPublisher).publish(saved, client);
    }

    @Test
    void create_ShouldReturnExisting_WhenIdempotentKeyMatches() {
        NotificationEntity existing = new NotificationEntity();
        existing.setId(UUID.randomUUID());
        existing.setExternalRequestId("ext-2");

        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-2"))
            .thenReturn(Optional.of(existing));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-2");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Subject");

        CreateNotificationResult result = service.create(request, principal);

        assertThat(result.isReplayed()).isTrue();
        assertThat(result.getEntity()).isSameAs(existing);
        verify(notificationRepository, never()).save(any());
        verify(taskPublisher, never()).publish(any(), any());
    }

    @Test
    void create_ShouldValidateTemplate_WhenTemplateCodeProvided() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-3"))
            .thenReturn(Optional.empty());
        when(clientRepository.findById(principal.clientId())).thenReturn(Optional.of(client()));
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-3");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setTemplateCode("WELCOME_TEMPLATE");

        CreateNotificationResult result = service.create(request, principal);

        assertThat(result.isReplayed()).isFalse();
        verify(templateService).requireActiveByCode("WELCOME_TEMPLATE");
    }

    @Test
    void create_ShouldThrow_WhenTemplateInactive() {
        doThrow(new TemplateInactiveException("WELCOME_TEMPLATE"))
            .when(templateService)
            .requireActiveByCode("WELCOME_TEMPLATE");

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-4");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setTemplateCode("WELCOME_TEMPLATE");

        assertThatThrownBy(() -> service.create(request, principal))
            .isInstanceOf(TemplateInactiveException.class);
        verify(notificationRepository, never()).save(any());
        verify(taskPublisher, never()).publish(any(), any());
    }

    @Test
    void create_ShouldRejectUnsupportedChannel() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-7");
        request.setChannel(Channel.SMS);
        request.setTo("user@example.com");
        request.setSubject("Subject");

        assertThatThrownBy(() -> service.create(request, principal))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only email channel");

        verify(notificationRepository, never()).save(any());
        verify(taskPublisher, never()).publish(any(), any());
    }

    @Test
    void create_ShouldFailWhenClientMissing() {
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-8"))
            .thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(clientRepository.findById(principal.clientId())).thenReturn(Optional.empty());

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-8");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Subject");

        assertThatThrownBy(() -> service.create(request, principal))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Client not found");
    }

    @Test
    void create_ShouldThrow_WhenTemplateMissing() {
        doThrow(new TemplateCodeNotFoundException("MISSING"))
            .when(templateService)
            .requireActiveByCode("MISSING");

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-5");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setTemplateCode("MISSING");

        assertThatThrownBy(() -> service.create(request, principal))
            .isInstanceOf(TemplateCodeNotFoundException.class);
        verify(notificationRepository, never()).save(any());
        verify(taskPublisher, never()).publish(any(), any());
    }

    @Test
    void create_ShouldPropagateSendAtValidationErrors() {
        Instant requested = Instant.parse("2024-01-02T00:00:00Z");
        Instant earliest = Instant.parse("2024-01-01T00:00:00Z");
        Instant latest = Instant.parse("2025-01-01T00:00:00Z");

        doThrow(new SendAtWindowException(requested, earliest, latest))
            .when(notificationPolicy)
            .validateSendAt(eq(requested), any());

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-6");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSendAt(requested);

        assertThatThrownBy(() -> service.create(request, principal))
            .isInstanceOf(SendAtWindowException.class);
        verify(notificationRepository, never()).save(any());
        verify(taskPublisher, never()).publish(any(), any());
    }

    @Test
    void recordDeliveryResult_ShouldUpdateStatusAndAttempts() {
        Instant attemptedAt = Instant.parse("2024-01-01T00:00:05Z");
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus(NotificationStatus.CREATED);
        entity.setAttempts(0);
        entity.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));

        when(notificationRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(deliveryAttemptRepository.findByNotificationIdAndAttempt(entity.getId(), 2))
            .thenReturn(Optional.empty());
        when(deliveryAttemptRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationEntity updated =
            service.recordDeliveryResult(
                entity.getId(), NotificationStatus.SENT, 2, attemptedAt, "SMTP", "ok");

        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(updated.getAttempts()).isEqualTo(2);
        assertThat(updated.getUpdatedAt()).isEqualTo(attemptedAt);
    }

    @Test
    void recordDeliveryResult_ShouldThrow_WhenNotificationMissing() {
        UUID missingId = UUID.randomUUID();
        when(notificationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () ->
                service.recordDeliveryResult(
                    missingId, NotificationStatus.SENT, 1, Instant.now(), null, null))
            .isInstanceOf(
                com.example.notifi.api.core.notification.exceptions.NotificationNotFoundException
                    .class);
    }

    @Test
    void recordDeliveryResult_ShouldRejectInvalidAttemptOrStatus() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(
                () ->
                    service.recordDeliveryResult(
                        id, NotificationStatus.CREATED, 0, Instant.now(), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt must be");
    }

    @Test
    void recordDeliveryResult_ShouldNotUpdateWhenExistingAttemptIsNewer() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus(NotificationStatus.FAILED);
        entity.setAttempts(2);
        entity.setUpdatedAt(now.plusSeconds(5));

        NotificationDeliveryAttemptEntity existingAttempt = new NotificationDeliveryAttemptEntity();
        existingAttempt.setAttempt(2);
        existingAttempt.setNotificationId(entity.getId());
        existingAttempt.setStatus(com.example.notifi.common.model.DeliveryStatus.FAILED);
        existingAttempt.setOccurredAt(now.plusSeconds(10));
        existingAttempt.setErrorMessage("previous");

        when(notificationRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(deliveryAttemptRepository.findByNotificationIdAndAttempt(entity.getId(), 2))
            .thenReturn(Optional.of(existingAttempt));

        NotificationEntity updated =
            service.recordDeliveryResult(
                entity.getId(), NotificationStatus.SENT, 2, now, "SMTP", "new error");

        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(existingAttempt.getErrorMessage()).isEqualTo("previous");
        assertThat(updated.getUpdatedAt()).isEqualTo(now.plusSeconds(5));
        verify(deliveryAttemptRepository, never()).save(existingAttempt);
    }

    @Test
    void recordDeliveryResult_ShouldTruncateLongErrorMessage() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus(NotificationStatus.QUEUED);
        entity.setAttempts(0);

        when(notificationRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(deliveryAttemptRepository.findByNotificationIdAndAttempt(entity.getId(), 1))
            .thenReturn(Optional.empty());
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String longMessage = "x".repeat(1100);

        service.recordDeliveryResult(
            entity.getId(), NotificationStatus.FAILED, 1, now, "SMTP", longMessage);

        ArgumentCaptor<NotificationDeliveryAttemptEntity> attemptCaptor =
            ArgumentCaptor.forClass(NotificationDeliveryAttemptEntity.class);
        verify(deliveryAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getErrorMessage().length()).isEqualTo(1024);
    }

    @Test
    void recordDeliveryResult_ShouldNotOverwriteSentWithFailed() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus(NotificationStatus.SENT);
        entity.setAttempts(2);
        entity.setUpdatedAt(now);

        when(notificationRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(deliveryAttemptRepository.findByNotificationIdAndAttempt(entity.getId(), 1))
            .thenReturn(Optional.empty());
        when(deliveryAttemptRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationEntity updated =
            service.recordDeliveryResult(
                entity.getId(), NotificationStatus.FAILED, 1, now.minusSeconds(5), null, "error");

        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(updated.getUpdatedAt()).isEqualTo(now);
        assertThat(updated.getAttempts()).isEqualTo(2);
    }
}
