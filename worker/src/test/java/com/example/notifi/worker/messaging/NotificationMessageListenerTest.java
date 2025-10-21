/*package com.example.notifi.worker.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static com.example.notifi.worker.model.AmqpConstants.DLQ_ROUTING_KEY;
import static com.example.notifi.worker.model.AmqpConstants.RETRY_ROUTING_KEY;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notifi.common.model.NotificationStatus;
import com.example.notifi.common.model.NotificationTaskPayload;
import com.example.notifi.worker.config.WorkerProperties;
import com.example.notifi.worker.model.DeliveryEntity;
import com.example.notifi.worker.model.NotificationEntity;
import com.example.notifi.worker.repo.DeliveryRepository;
import com.example.notifi.worker.repo.NotificationRepository;
import com.example.notifi.worker.consumer.SimpleSmtpSender;
import com.example.notifi.worker.amqp.RetryPolicy;
import com.example.notifi.worker.webhook.WebhookDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationMessageListenerTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private EmailSender emailSender;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private WebhookDispatcher webhookDispatcher;
    @Captor private ArgumentCaptor<NotificationTaskPayload> payloadCaptor;

    private NotificationMessageListener listener;
    private WorkerProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new WorkerProperties();
        properties.getRetry().setMaxAttempts(3);
        listener = new NotificationMessageListener(
                notificationRepository,
                deliveryRepository,
                emailSender,
                new RetryPolicy(),
                properties,
                new SimpleMeterRegistry(),
                rabbitTemplate,
                webhookDispatcher,
                objectMapper,
                Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void movesMessageToDlqWhenAttemptsExhausted() throws Exception {
        UUID notificationId = UUID.randomUUID();
        NotificationEntity entity = new NotificationEntity();
        entity.setId(notificationId);
        entity.setClientId("client");
        entity.setExternalRequestId("req");
        entity.setStatus(NotificationStatus.QUEUED);
        entity.setChannel("email");
        entity.setRecipient("test@example.com");
        entity.setSubject("Hello");
        entity.setAttempts(2);
        entity.setTraceId("trace");
        entity.setCreatedAt(Instant.parse("2023-12-31T23:59:00Z"));

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(entity));
        when(deliveryRepository.findById(any())).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(DeliveryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("smtp down")).when(emailSender).send(any());

        NotificationTaskPayload payload = new NotificationTaskPayload(
                NotificationTaskPayload.SCHEMA_VERSION,
                notificationId,
                entity.getClientId(),
                entity.getExternalRequestId(),
                "email",
                entity.getRecipient(),
                entity.getSubject(),
                null,
                null,
                3,
                entity.getTraceId(),
                null,
                null);

        listener.handle(payload);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(DLQ_ROUTING_KEY), payloadCaptor.capture());
        verify(rabbitTemplate, never()).convertAndSend(eq(EXCHANGE), eq(RETRY_ROUTING_KEY), any(), any());
        verify(webhookDispatcher).dispatch(any(NotificationEntity.class));
    }
}*/
