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
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.data.repository.NotificationRepository;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationRequest;
import com.example.notifi.common.model.Channel;
import java.time.Clock;
import java.time.Instant;
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
  @Mock private Clock clock;

  @InjectMocks private NotificationService service;

  private final ClientPrincipal principal =
      new ClientPrincipal(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Client", 10);

  @Test
  void create_ShouldPersistNotification_WhenSendAtDue() {
    Instant now = Instant.parse("2024-01-01T00:00:00Z");
    ClientEntity client = client();
    when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-1"))
        .thenReturn(Optional.empty());
    when(clock.instant()).thenReturn(now);
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

    ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
    verify(notificationRepository).save(captor.capture());

    NotificationEntity saved = captor.getValue();
    assertThat(saved.getClientId()).isEqualTo(principal.clientId());
    assertThat(saved.getSendAt()).isEqualTo(now);
    assertThat(saved.getStatus()).isEqualTo(NotificationStatus.CREATED);
    assertThat(saved.getSubject()).isEqualTo("Subject");
    assertThat(result.getEntity()).isSameAs(saved);
    verify(taskPublisher).publish(saved, client);
  }

  @Test
  void create_ShouldUseRequestedSendAt_WhenScheduledInFuture() {
    Instant now = Instant.parse("2024-01-01T00:00:00Z");
    Instant scheduled = now.plusSeconds(600);
    ClientEntity client = client();
    when(notificationRepository.findByClientIdAndExternalRequestId(principal.clientId(), "ext-2"))
        .thenReturn(Optional.empty());
    when(clock.instant()).thenReturn(now);
    when(clientRepository.findById(principal.clientId())).thenReturn(Optional.of(client));
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
    verify(taskPublisher).publish(saved, client);
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
    verify(taskPublisher, never()).publish(any(), any());
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
    verify(taskPublisher, never()).publish(any(), any());
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
    verify(taskPublisher, never()).publish(any(), any());
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
    verify(taskPublisher, never()).publish(any(), any());
  }

  @Test
  void recordDeliveryResult_ShouldUpdateStatusAttemptsAndTimestamp() {
    Instant now = Instant.parse("2024-01-01T00:00:00Z");
    NotificationEntity entity = new NotificationEntity();
    entity.setId(UUID.randomUUID());
    entity.setStatus(NotificationStatus.QUEUED);
    entity.setAttempts(1);
    Instant attemptedAt = now.plusSeconds(5);

    when(notificationRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(notificationRepository.save(any(NotificationEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    NotificationEntity updated =
        service.recordDeliveryResult(entity.getId(), NotificationStatus.SENT, attemptedAt, null);

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
                    missingId, NotificationStatus.SENT, Instant.now(), null))
        .isInstanceOf(
            com.example.notifi.api.core.notification.exceptions.NotificationNotFoundException
                .class);
  }

  private ClientEntity client() {
    ClientEntity client = new ClientEntity();
    client.setId(principal.clientId());
    client.setWebhookUrl("http://webhook");
    client.setWebhookSecret("secret");
    return client;
  }
}
