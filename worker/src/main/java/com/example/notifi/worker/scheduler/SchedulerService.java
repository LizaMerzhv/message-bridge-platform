package com.example.notifi.worker.scheduler;

import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.example.notifi.worker.amqp.AmqpPublisher;
import com.example.notifi.worker.config.WorkerProperties;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.model.NotificationEntity;
import com.example.notifi.worker.model.NotificationMessageMapper;
import com.example.notifi.worker.repo.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulerService {
  private final NotificationRepository notificationRepository;
  private final NotificationMessageMapper mapper;
  private final AmqpPublisher publisher;
  private final WorkerProperties properties;
  private final Clock clock;
  private final WorkerMetrics metrics;

  public SchedulerService(
      NotificationRepository notificationRepository,
      NotificationMessageMapper mapper,
      AmqpPublisher publisher,
      WorkerProperties properties,
      Clock clock,
      WorkerMetrics metrics) {
    this.notificationRepository = notificationRepository;
    this.mapper = mapper;
    this.publisher = publisher;
    this.properties = properties;
    this.clock = clock;
    this.metrics = metrics;
  }

  @Scheduled(fixedDelayString = "${notifi.scheduler.scan-interval-ms:2000}")
  @Transactional
  public void publishDueNotifications() {
    Instant now = clock.instant();
    List<NotificationEntity> due =
        notificationRepository.lockCreatedBefore(now, properties.getScheduler().getBatchSize());
    if (due.isEmpty()) {
      return;
    }

    for (NotificationEntity notification : due) {
        int attempt = 1; // initial attempt for worker processing
        notification.markQueued(now, attempt);
        NotificationTaskMessage message = mapper.toMessage(notification, attempt);
        publisher.publishTask(message);
        metrics.incrementNotificationsQueued();
    }
  }
}
