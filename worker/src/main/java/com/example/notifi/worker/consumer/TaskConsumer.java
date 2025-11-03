package com.example.notifi.worker.consumer;

import com.example.notifi.common.messaging.NotificationTaskMessage; // use DTO emitted by API service
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
        public void handle(NotificationTaskMessage message) {
            NotificationEntity notification =
                notificationRepository
                    .findById(message.notificationId())
            .orElseThrow(() -> new IllegalStateException("Notification not found: " + message.notificationId())); // worker DB isolation guard

            DeliveryEntity delivery =
                deliveryRepository.save(DeliveryEntity.create(notification, message.attempt())); // persist worker-owned delivery record
            Instant now = clock.instant();
            try {
                smtpSender.send(message);
                delivery.markSent();
                notification.markSent(now, message.attempt());
                metrics.incrementDeliveriesSent(message.channel());
                metrics.recordSendLatency(notification.getCreatedAt(), now);
                webhookDispatcher.dispatch(notification);
            } catch (Exception ex) {
                delivery.markFailed(ex.getMessage());
                RetryDecision decision = retryPolicy.evaluate(message.attempt());
                metrics.incrementDeliveriesFailed(message.channel());
                metrics.recordFailureLatency(notification.getCreatedAt(), now);
                if (decision.shouldRetry()) {
                    NotificationTaskMessage retryMessage =
                        new NotificationTaskMessage(
                            message.notificationId(), // propagate original notification id
                            message.clientId(), // keep client context for webhook
                            message.externalRequestId(), // maintain idempotency information
                            message.channel(), // ensure channel stays unchanged
                            message.recipient(), // keep recipient for retry send
                            message.subject(), // reuse subject across attempts
                            message.templateCode(), // keep template reference stable
                            message.variables(), // reuse serialized variables
                            message.sendAt(), // original scheduling time stays constant
                            message.createdAt(), // preserve creation timestamp for metrics
                            decision.nextAttempt(), // bump attempt number for retry
                            message.traceId(), // correlate with API request id
                            message.webhookUrl(), // keep webhook target
                            message.webhookSecret()); // keep webhook secret
                    publisher.publishRetry(retryMessage, decision.ttl(), decision.additionalDelay());
                    notification.markQueued(now, message.attempt());
                } else {
                    notification.markFailed(now, message.attempt());
                    publisher.publishDlq(message);
                    webhookDispatcher.dispatch(notification);
                }
            }
        }

        public interface SmtpSender {
            void send(NotificationTaskMessage message) throws Exception;
        }
    }
