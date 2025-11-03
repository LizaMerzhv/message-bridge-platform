package com.example.notifi.worker.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notifi.common.messaging.NotificationTaskMessage; // use shared DTO in tests
import com.example.notifi.common.model.Channel;
import com.example.notifi.common.model.NotificationStatus;
import com.example.notifi.worker.amqp.AmqpPublisher;
import com.example.notifi.worker.amqp.RetryPolicy;
import com.example.notifi.worker.amqp.RetryPolicy.RetryDecision;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.model.NotificationEntity;
import com.example.notifi.worker.repo.DeliveryRepository;
import com.example.notifi.worker.repo.NotificationRepository;
import com.example.notifi.worker.webhook.WebhookDispatcher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskConsumerTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private TaskConsumer.SmtpSender smtpSender;
    @Mock private RetryPolicy retryPolicy;
    @Mock private AmqpPublisher amqpPublisher;
    @Mock private WebhookDispatcher webhookDispatcher;

    private WorkerMetrics metrics;
    private Clock clock;
    private TaskConsumer consumer;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        metrics = new WorkerMetrics(new SimpleMeterRegistry());
        consumer =
            new TaskConsumer(
                notificationRepository,
                deliveryRepository,
                smtpSender,
                retryPolicy,
                amqpPublisher,
                metrics,
                clock,
                webhookDispatcher);
    }

    @Test
    void successMarksNotificationAndDelivery() throws Exception {
        NotificationEntity notification = createNotification();
        when(notificationRepository.findById(notification.getId())).thenReturn(java.util.Optional.of(notification));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationTaskMessage message = createMessage(notification.getId(), 1);

        consumer.handle(message);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(webhookDispatcher).dispatch(notification);
        double sentCount =
            metrics
                .registry()
                .find("deliveries_sent_total")
                .tags("channel", Channel.EMAIL.metricTag())
                .counter()
                .count();
        assertThat(sentCount).isEqualTo(1.0d);
    }

    @Test
    void failureRetriesWhenAttemptsRemain() throws Exception {
        NotificationEntity notification = createNotification();
        when(notificationRepository.findById(notification.getId())).thenReturn(java.util.Optional.of(notification));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("smtp error")).when(smtpSender).send(any());
        when(retryPolicy.evaluate(1))
            .thenReturn(RetryDecision.retry(2, java.time.Duration.ofSeconds(8), java.time.Duration.ZERO));

        NotificationTaskMessage message = createMessage(notification.getId(), 1);
        consumer.handle(message);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        NotificationTaskMessage expectedRetry =
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
                2,
                message.traceId(),
                message.webhookUrl(),
                message.webhookSecret());
        verify(amqpPublisher)
            .publishRetry(expectedRetry, java.time.Duration.ofSeconds(8), java.time.Duration.ZERO);
        assertThat(metrics.registry().get("deliveries_failed_total").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void failureRoutesToDlqAfterMaxAttempts() throws Exception {
        NotificationEntity notification = createNotification();
        when(notificationRepository.findById(notification.getId())).thenReturn(java.util.Optional.of(notification));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("smtp error")).when(smtpSender).send(any());
        when(retryPolicy.evaluate(3)).thenReturn(RetryDecision.noRetry());

        NotificationTaskMessage message = createMessage(notification.getId(), 3);
        consumer.handle(message);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(amqpPublisher).publishDlq(message);
        verify(webhookDispatcher).dispatch(notification);
        assertThat(metrics.registry().get("deliveries_failed_total").counter().count()).isEqualTo(1.0d);
    }

    private NotificationEntity createNotification() {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setClientId(UUID.randomUUID());
        entity.setExternalRequestId("ext-" + entity.getId());
        entity.setSendAt(clock.instant());
        entity.setStatus(NotificationStatus.QUEUED);
        entity.setChannel(Channel.EMAIL);
        entity.setToAddress("user@example.com");
        entity.setSubject("Subject");
        entity.setTraceId("trace");
        entity.setWebhookSecret("secret");
        entity.setWebhookUrl("http://example.com");
        entity.setCreatedAt(clock.instant());
        return entity;
    }

    private NotificationTaskMessage createMessage(UUID notificationId, int attempt) {
        return new NotificationTaskMessage(
            notificationId,
            UUID.randomUUID(),
            "ext",
            Channel.EMAIL,
            "user@example.com",
            "Subject",
            null,
            null,
            clock.instant(),
            clock.instant(),
            attempt,
            "trace",
            "http://example.com",
            "secret");
    }
}
