package com.example.notifi.worker.consumer;

import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.example.notifi.common.model.NotificationStatus;
import com.example.notifi.worker.model.NotificationEntity;
import com.example.notifi.worker.repo.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to API events and persists a worker-local copy of notifications.
 */
@Component
public class NotificationIngestionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationIngestionListener.class);

    private final NotificationRepository notificationRepository;
    private final Clock clock;
    public NotificationIngestionListener(
        NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @RabbitListener(
        queues = "${notifi.amqp.ingest-queue:" + com.example.notifi.common.messaging.AmqpConstants.INGEST_QUEUE + "}",
        containerFactory = "taskListenerContainerFactory")
    @Transactional
    public void handle(NotificationTaskMessage message) {
        notificationRepository
            .findById(message.notificationId())
            .ifPresentOrElse(
                existing -> log.debug("Notification {} already ingested", existing.getId()),
                () -> notificationRepository.save(mapToEntity(message)));
    }

    private NotificationEntity mapToEntity(NotificationTaskMessage message) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(message.notificationId());
        entity.setClientId(message.clientId());
        entity.setExternalRequestId(message.externalRequestId());
        entity.setSendAt(message.sendAt() != null ? message.sendAt() : clock.instant());
        entity.setStatus(NotificationStatus.CREATED);
        entity.setChannel(message.channel());
        entity.setToAddress(message.recipient());
        entity.setSubject(message.subject());
        entity.setTemplateCode(message.templateCode());
        entity.setVariables(message.variables());
        entity.setTraceId(message.traceId());
        entity.setWebhookUrl(message.webhookUrl());
        entity.setWebhookSecret(message.webhookSecret());
        entity.setAttempts(0);
        Instant createdAt = message.createdAt() != null ? message.createdAt() : clock.instant();
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }
}
