package com.example.notifi.worker.consumer;

import com.example.notifi.worker.model.NotificationMessage;
import com.example.notifi.worker.amqp.AmqpPublisher;
import com.example.notifi.worker.amqp.RetryPolicy;
import com.example.notifi.worker.amqp.RetryPolicy.RetryDecision;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.model.DeliveryEntity;
import com.example.notifi.worker.model.NotificationEntity;
import com.example.notifi.worker.repo.DeliveryRepository;
import com.example.notifi.worker.repo.NotificationRepository;
import com.example.notifi.worker.webhook.WebhookDispatcher;
import java.time.Clock;
import java.time.Instant;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskConsumer {
  private final NotificationRepository notificationRepository;
  private final DeliveryRepository deliveryRepository;
  private final SmtpSender smtpSender;
  private final RetryPolicy retryPolicy;
  private final AmqpPublisher publisher;
  private final WorkerMetrics metrics;
  private final Clock clock;
  private final WebhookDispatcher webhookDispatcher;

  public TaskConsumer(
      NotificationRepository notificationRepository,
      DeliveryRepository deliveryRepository,
      SmtpSender smtpSender,
      RetryPolicy retryPolicy,
      AmqpPublisher publisher,
      WorkerMetrics metrics,
      Clock clock,
      WebhookDispatcher webhookDispatcher) {
    this.notificationRepository = notificationRepository;
    this.deliveryRepository = deliveryRepository;
    this.smtpSender = smtpSender;
    this.retryPolicy = retryPolicy;
    this.publisher = publisher;
    this.metrics = metrics;
    this.clock = clock;
    this.webhookDispatcher = webhookDispatcher;
  }

  @RabbitListener(
      queues = "${notifi.amqp.tasks-queue:notify.tasks}",
      containerFactory = "taskListenerContainerFactory")
  @Transactional
  public void handle(NotificationMessage message) {
    NotificationEntity notification =
        notificationRepository
            .findById(message.notificationId())
            .orElseThrow(() -> new IllegalStateException("Notification not found: " + message.notificationId()));

    DeliveryEntity delivery = deliveryRepository.save(DeliveryEntity.create(notification, message.attempt()));
    Instant now = clock.instant();
    try {
      smtpSender.send(message);
      delivery.markSent();
      notification.markSent(now);
      metrics.incrementDeliveriesSent(message.channel());
      metrics.recordSendLatency(notification.getCreatedAt(), now);
      webhookDispatcher.dispatch(notification);
    } catch (Exception ex) {
      delivery.markFailed(ex.getMessage());
      RetryDecision decision = retryPolicy.evaluate(message.attempt());
      metrics.incrementDeliveriesFailed(message.channel());
      metrics.recordFailureLatency(notification.getCreatedAt(), now);
      if (decision.shouldRetry()) {
        NotificationMessage retryMessage = message.withAttempt(decision.nextAttempt());
        publisher.publishRetry(retryMessage, decision.ttl(), decision.additionalDelay());
        notification.markQueued(now);
      } else {
        notification.markFailed(now);
        publisher.publishDlq(message);
        webhookDispatcher.dispatch(notification);
      }
    }
  }

  public interface SmtpSender {
    void send(NotificationMessage message) throws Exception;
  }
}
