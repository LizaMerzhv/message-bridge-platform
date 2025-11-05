package com.example.notificationapp.adminui.model;

import java.time.OffsetDateTime;

public record DeliveryAttempt(
        int attempt,
        String status,
        OffsetDateTime timestamp,
        String channel,
        String to,
        String subject,
        String errorCode,
        String errorMessage) {}
