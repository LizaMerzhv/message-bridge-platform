package com.example.notificationapp.adminui.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record NotificationDetail(
        String id,
        String channel,
        String to,
        String status,
        String subject,
        String templateCode,
        Map<String, Object> variables,
        String externalRequestId,
        OffsetDateTime sendAt,
        OffsetDateTime sendAtEffective,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        int attempts,
        List<DeliveryAttempt> deliveries) {}
