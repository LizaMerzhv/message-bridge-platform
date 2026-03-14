package com.example.notifi.api.core.notification;

import com.example.notifi.api.data.entity.ClientEntity;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.OutboxEntity;
import com.example.notifi.api.data.entity.OutboxStatus;
import com.example.notifi.api.data.repository.OutboxRepository;
import com.example.notifi.api.messaging.NotificationTaskMessage;
import com.example.notifi.api.model.Channel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Stores notification creation events in the outbox for reliable publishing to the worker service.
 */
@Component
public class NotificationTaskPublisher {

  private static final Logger log = LoggerFactory.getLogger(NotificationTaskPublisher.class);

  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public NotificationTaskPublisher(
      OutboxRepository outboxRepository, ObjectMapper objectMapper, Clock clock) {
    this.outboxRepository = outboxRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public void publish(NotificationEntity entity, ClientEntity client) {
    Channel channel = Channel.from(entity.getChannel());
    String messageKey = "notification:" + entity.getId();

    Optional<OutboxEntity> existing = outboxRepository.findByMessageKey(messageKey);
    if (existing.isPresent()) {
      log.debug("Notification {} already stored in outbox", entity.getId());
      return;
    }

    NotificationTaskMessage payload =
        new NotificationTaskMessage(
            "v1",
            UUID.randomUUID(),
            entity.getId(),
            entity.getClientId(),
            entity.getExternalRequestId(),
            channel,
            entity.getTo(),
            entity.getSubject(),
            entity.getTemplateCode(),
            entity.getVariables(),
            entity.getSendAt(),
            entity.getCreatedAt(),
            0,
            MDC.get("traceId"),
            client.getWebhookUrl(),
            client.getWebhookSecret());

    String payloadJson = serialize(payload);
    Instant now = clock.instant();

    OutboxEntity outbox = new OutboxEntity();
    outbox.setId(UUID.randomUUID());
    outbox.setMessageKey(messageKey);
    outbox.setEventType("NOTIFICATION_TASK");
    outbox.setPayload(payloadJson);
    outbox.setStatus(OutboxStatus.PENDING);
    outbox.setAttempts(0);
    outbox.setCreatedAt(now);
    outbox.setUpdatedAt(now);

    try {
      outboxRepository.save(outbox);
      log.info(
          "Stored notification {} in outbox with status {} scheduled at {}",
          entity.getId(),
          entity.getStatus(),
          entity.getSendAt());
    } catch (DataIntegrityViolationException ex) {
      log.debug("Outbox record already exists for notification {}", entity.getId(), ex);
    }
  }

  private String serialize(NotificationTaskMessage payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize notification task payload", e);
    }
  }
}
