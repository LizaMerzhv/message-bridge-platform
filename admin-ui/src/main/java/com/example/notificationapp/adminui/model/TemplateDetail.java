package com.example.notificationapp.adminui.model;

import java.time.OffsetDateTime;

public record TemplateDetail(
        String code,
        String status,
        String subject,
        String bodyHtml,
        String bodyText,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
