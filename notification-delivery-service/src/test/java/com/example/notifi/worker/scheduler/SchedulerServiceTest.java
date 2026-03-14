package com.example.notifi.worker.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.notifi.worker.amqp.AmqpPublisher;
import com.example.notifi.worker.config.WorkerProperties;
import com.example.notifi.worker.data.entity.NotificationEntity;
import com.example.notifi.worker.data.entity.NotificationMessageMapper;
import com.example.notifi.worker.data.repository.NotificationRepository;
import com.example.notifi.worker.messaging.NotificationTaskMessage;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.model.Channel;
import com.example.notifi.worker.model.NotificationStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private AmqpPublisher amqpPublisher;

  private WorkerProperties properties;
  private WorkerMetrics metrics;
  private Clock clock;
  private SchedulerService schedulerService;

  @BeforeEach
  void setUp() {
    properties = new WorkerProperties();
    properties.getScheduler().setBatchSize(10);
    metrics = new WorkerMetrics(new SimpleMeterRegistry());
    clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    schedulerService =
        new SchedulerService(
            notificationRepository,
            new NotificationMessageMapper(),
            amqpPublisher,
            properties,
            clock,
            metrics);
  }

  @Test
  void publishesDueNotifications() {
    NotificationEntity entity = createNotification(clock.instant().minusSeconds(10));
    when(notificationRepository.lockCreatedBefore(clock.instant(), 10)).thenReturn(List.of(entity));

    schedulerService.publishDueNotifications();

    assertThat(entity.getStatus()).isEqualTo(NotificationStatus.QUEUED);
    ArgumentCaptor<NotificationTaskMessage> captor =
        ArgumentCaptor.forClass(NotificationTaskMessage.class);
    verify(amqpPublisher).publishTask(captor.capture());
    assertThat(captor.getValue().attempt()).isEqualTo(1);
    assertThat(metrics.registry().get("notifications_queued_total").counter().count())
        .isEqualTo(1.0d);
  }

  @Test
  void handlesEmptyBatch() {
    when(notificationRepository.lockCreatedBefore(clock.instant(), 10))
        .thenReturn(Collections.emptyList());

    schedulerService.publishDueNotifications();

    verify(amqpPublisher, never()).publishTask(any());
  }

  @Test
  void usesConfiguredBatchSize() {
    properties.getScheduler().setBatchSize(3);
    schedulerService =
        new SchedulerService(
            notificationRepository,
            new NotificationMessageMapper(),
            amqpPublisher,
            properties,
            clock,
            metrics);

    schedulerService.publishDueNotifications();

    verify(notificationRepository).lockCreatedBefore(clock.instant(), 3);
  }

  private NotificationEntity createNotification(Instant sendAt) {
    NotificationEntity entity = new NotificationEntity();
    entity.setId(UUID.randomUUID());
    entity.setClientId(UUID.randomUUID());
    entity.setExternalRequestId("ext-" + entity.getId());
    entity.setSendAt(sendAt);
    entity.setStatus(NotificationStatus.CREATED);
    entity.setChannel(Channel.EMAIL);
    entity.setToAddress("user@example.com");
    entity.setSubject("Hello");
    entity.setTraceId("trace-" + entity.getId());
    entity.setWebhookSecret("secret");
    entity.setWebhookUrl("http://example.com");
    entity.setCreatedAt(clock.instant());
    return entity;
  }
}
