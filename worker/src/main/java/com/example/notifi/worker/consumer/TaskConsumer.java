package com.example.notifi.worker.consumer;

import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.example.notifi.common.model.NotificationStatus;
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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskConsumer.class);

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
    public void handle(NotificationTaskMessage message) {
        UUID notificationId = message.notificationId();
        log.info("Received task for notification {} (attempt {})", notificationId, message.attempt());
        Instant now = clock.instant();

        try {
            NotificationEntity notification =
                notificationRepository
                    .findById(notificationId)
                    .orElseThrow(() ->
                        new IllegalStateException("Notification not found: " + notificationId));

            if (notification.getStatus() == NotificationStatus.SENT) {
                log.info("Notification {} already SENT, skipping", notificationId);
                return;
            }

            DeliveryEntity delivery =
                deliveryRepository.save(DeliveryEntity.create(notification, message.attempt()));

            try {
                smtpSender.send(message);

                delivery.markSent();
                notification.markSent(now, message.attempt());

                metrics.incrementDeliveriesSent(message.channel());
                metrics.recordSendLatency(notification.getCreatedAt(), now);

                webhookDispatcher.dispatch(notification);

            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage != null && errorMessage.length() > 512) {
                    errorMessage = errorMessage.substring(0, 512);
                }
                delivery.markFailed(errorMessage);

                RetryDecision decision = retryPolicy.evaluate(message.attempt());

                metrics.incrementDeliveriesFailed(message.channel());
                metrics.recordFailureLatency(notification.getCreatedAt(), now);

                if (decision.shouldRetry()) {
                    NotificationTaskMessage retryMessage =
                        new NotificationTaskMessage(
                            message.notificationId(),
                            message.clientId(),
                            message.externalRequestId(),
                            message.channel(),
                            message.recipient(),
                            message.subject(),
                            message.templateCode(),
                            message.variables(),
                            message.sendAt(),
                            message.createdAt(),
                            decision.nextAttempt(),
                            message.traceId(),
                            message.webhookUrl(),
                            message.webhookSecret());

                    publisher.publishRetry(retryMessage, decision.ttl(), decision.additionalDelay());
                    notification.markQueued(now, decision.nextAttempt());
                } else {
                    notification.markFailed(now, message.attempt());
                    publisher.publishDlq(message);
                    webhookDispatcher.dispatch(notification);
                }
            }

        } catch (Exception e) {
            log.error("Fatal error while processing notification {}", notificationId, e);
        }
    }

    public interface SmtpSender {
        void send(NotificationTaskMessage message) throws Exception;
    }
}
