package com.example.notifi.common.messaging;

import com.example.notifi.common.model.Channel;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationTaskMessage(
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
