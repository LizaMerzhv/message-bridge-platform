package com.example.notifi.api.core.notification;

import com.example.notifi.api.core.notification.exception.SendAtWindowException;
import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.core.template.exception.TemplateCodeNotFoundException;
import com.example.notifi.api.core.template.exception.TemplateInactiveException;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.data.repository.NotificationRepository;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.notification.dto.CreateNotificationRequest;
import com.example.notifi.api.data.entity.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationPolicy notificationPolicy;
    @Mock private TemplateService templateService;
    @Mock private Clock clock;

    @InjectMocks private NotificationService service;

    private final ClientPrincipal principal =
            new ClientPrincipal(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Client", 10);

    @Test
    void create_ShouldPersistNotification_WhenSendAtDue() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-1"))
            .thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(now);
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-1");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Subject");

        CreateNotificationResult result = service.create(request, principal);
        assertThat(result.isReplayed()).isFalse();

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());

        NotificationEntity saved = captor.getValue();
        assertThat(saved.getClientId()).isEqualTo(principal.clientId());
        assertThat(saved.getSendAt()).isEqualTo(now);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.CREATED);
        assertThat(saved.getSubject()).isEqualTo("Subject");
        assertThat(result.getEntity()).isSameAs(saved);
    }

    @Test
    void create_ShouldUseRequestedSendAt_WhenScheduledInFuture() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Instant scheduled = now.plusSeconds(600);
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-2"))
            .thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(now);
        when(notificationRepository.save(any(NotificationEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-2");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Subject");
        request.setSendAt(scheduled);

        CreateNotificationResult result = service.create(request, principal);
        assertThat(result.isReplayed()).isFalse();

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertThat(saved.getSendAt()).isEqualTo(scheduled);
        assertThat(result.getEntity()).isSameAs(saved);
    }


    @Test
    void create_ShouldReturnExisting_WhenIdempotent() {
        NotificationEntity existing = new NotificationEntity();
        existing.setId(UUID.randomUUID());
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-3"))
                .thenReturn(Optional.of(existing));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-3");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Subject");

        CreateNotificationResult result = service.create(request, principal);
        assertThat(result.isReplayed()).isTrue();
        assertThat(result.getEntity()).isSameAs(existing);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrow_WhenTemplateMissing() {
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-4"))
                .thenReturn(Optional.empty());
        doThrow(new TemplateCodeNotFoundException("CODE"))
                .when(templateService)
                .requireActiveByCode("CODE");

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-4");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setTemplateCode("CODE");

        assertThatThrownBy(() -> service.create(request, principal))
                .isInstanceOf(TemplateCodeNotFoundException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrow_WhenTemplateInactive() {
        when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-5"))
                .thenReturn(Optional.empty());
        doThrow(new TemplateInactiveException("CODE"))
                .when(templateService)
                .requireActiveByCode("CODE");

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-5");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setTemplateCode("CODE");

        assertThatThrownBy(() -> service.create(request, principal))
                .isInstanceOf(TemplateInactiveException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void create_ShouldPropagateSendAtWindowException() {
        doThrow(new SendAtWindowException(Instant.EPOCH, Instant.EPOCH, Instant.EPOCH))
                .when(notificationPolicy)
                .validateSendAt(any(), eq(clock));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("ext-6");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Subject");

        assertThatThrownBy(() -> service.create(request, principal))
                .isInstanceOf(SendAtWindowException.class);
        verify(notificationRepository, never()).save(any());
    }
}
