package com.example.notificationapp.adminui.model;

import java.time.OffsetDateTime;

public record NotificationSummary(
        String id, String to, String status, OffsetDateTime sendAt, OffsetDateTime createdAt, int attempts) {}
