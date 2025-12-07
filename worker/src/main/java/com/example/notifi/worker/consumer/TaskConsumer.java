package com.example.notifi.worker.consumer;

import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.example.notifi.common.model.NotificationStatus;
import com.example.notifi.worker.amqp.AmqpPublisher;
import com.example.notifi.worker.amqp.RetryPolicy;
import com.example.notifi.worker.amqp.RetryPolicy.RetryDecision;
import com.example.notifi.worker.data.entity.DeliveryEntity;
import com.example.notifi.worker.data.entity.NotificationEntity;
import com.example.notifi.worker.data.repository.DeliveryRepository;
import com.example.notifi.worker.data.repository.NotificationRepository;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.webhook.WebhookDispatcher;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
    private final NotificationApiClient notificationApiClient;

    public TaskConsumer(
        NotificationRepository notificationRepository,
        DeliveryRepository deliveryRepository,
        SmtpSender smtpSender,
        RetryPolicy retryPolicy,
        AmqpPublisher publisher,
        WorkerMetrics metrics,
        Clock clock,
        WebhookDispatcher webhookDispatcher,
        NotificationApiClient notificationApiClient) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.smtpSender = smtpSender;
        this.retryPolicy = retryPolicy;
        this.publisher = publisher;
        this.metrics = metrics;
        this.clock = clock;
        this.webhookDispatcher = webhookDispatcher;
        this.notificationApiClient = notificationApiClient;
    }

    @RabbitListener(queues = "${notifi.amqp.tasks-queue}")
    public void handle(NotificationTaskMessage message) {
        UUID notificationId = message.notificationId();
        int attempt = message.attempt();
        Instant now = clock.instant();

        log.info("Received task for notification {} (attempt {})", notificationId, attempt);

        try {
            NotificationEntity notification =
                notificationRepository
                    .findById(notificationId)
                    .orElseThrow(
                        () -> new IllegalStateException("Notification not found: " + notificationId));

            if (notification.getStatus() == NotificationStatus.SENT) {
                log.info("Notification {} already SENT, skipping", notificationId);
                return;
            }

            DeliveryEntity delivery =
                deliveryRepository.save(DeliveryEntity.create(notification, attempt));

            try {
                smtpSender.send(message);

                delivery.markSent();
                notification.markSent(now, attempt);

                metrics.incrementDeliveriesSent(message.channel());
                metrics.recordSendLatency(notification.getCreatedAt(), now);

                notifyApi(notification, NotificationStatus.SENT, attempt, null);

                webhookDispatcher.dispatch(notification);

            } catch (Exception ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage != null && errorMessage.length() > 512) {
                    errorMessage = errorMessage.substring(0, 512);
                }
                delivery.markFailed(errorMessage);

                RetryDecision decision = retryPolicy.evaluate(attempt);

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
                    notification.markFailed(now, attempt);
                    publisher.publishDlq(message);
                    webhookDispatcher.dispatch(notification);

                    notifyApi(notification, NotificationStatus.FAILED, attempt, ex);
                }
            }

        } catch (Exception e) {
            log.error("Fatal error while processing notification {}", notificationId, e);
        }
    }

    public NotificationApiClient getNotificationApiClient() {
        return notificationApiClient;
    }

    public interface SmtpSender {
        void send(NotificationTaskMessage message) throws Exception;
    }

    private void notifyApi(
        NotificationEntity notification,
        NotificationStatus status,
        int attempt,
        Exception ex) {

        int reportedAttempt = attempt + 1;

        String errorCode = (ex != null) ? "smtp" : null;
        String errorMessage = (ex != null) ? ex.getMessage() : null;

        try {
            notificationApiClient.sendDeliveryResult(
                notification.getId(),
                status,
                reportedAttempt,
                clock.instant(),
                errorCode,
                errorMessage);
        } catch (Exception e) {
            log.warn("Failed to notify API about delivery {}: {}", notification.getId(), e.getMessage());
        }
    }
}
