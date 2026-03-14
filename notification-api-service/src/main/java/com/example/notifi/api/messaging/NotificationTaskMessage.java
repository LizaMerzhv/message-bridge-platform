package com.example.notifi.api.messaging;

import com.example.notifi.api.model.Channel;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationTaskMessage(
    String schemaVersion,
    UUID eventId,
    UUID notificationId,
    UUID clientId,
    String externalRequestId,
    Channel channel,
    String recipient,
    String subject,
    String templateCode,
    Map<String, Object> variables,
    Instant sendAt,
    Instant createdAt,
    int attempt,
    String traceId,
    String webhookUrl,
    String webhookSecret) {}
