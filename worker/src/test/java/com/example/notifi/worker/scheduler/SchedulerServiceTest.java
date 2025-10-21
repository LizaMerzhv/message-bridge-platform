package com.example.notifi.worker.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notifi.common.model.Channel;
import com.example.notifi.worker.model.NotificationMessage;
import com.example.notifi.common.model.NotificationStatus;
import com.example.notifi.worker.amqp.AmqpPublisher;
import com.example.notifi.worker.config.WorkerProperties;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.model.NotificationEntity;
import com.example.notifi.worker.model.NotificationMessageMapper;
import com.example.notifi.worker.repo.NotificationRepository;
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

  private final WorkerProperties properties = new WorkerProperties();
  private final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
  private WorkerMetrics metrics;
  private SchedulerService schedulerService;

  @BeforeEach
  void setUp() {
    properties.getScheduler().setBatchSize(10);
    metrics = new WorkerMetrics(new SimpleMeterRegistry());
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
    ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
    verify(amqpPublisher).publishTask(captor.capture());
    assertThat(captor.getValue().attempt()).isEqualTo(1);
    assertThat(metrics.registry().get("notifications_queued_total").counter().count()).isEqualTo(1.0d);
  }

  @Test
  void handlesEmptyBatch() {
    when(notificationRepository.lockCreatedBefore(clock.instant(), 10)).thenReturn(Collections.emptyList());

    schedulerService.publishDueNotifications();

    verify(amqpPublisher, never()).publishTask(any());
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
    return entity;
  }
}
