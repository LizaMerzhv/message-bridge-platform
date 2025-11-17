package com.example.notifi.api.web.admin.template.dto;

import com.example.notifi.api.core.template.TemplateView;
import com.example.notifi.api.data.entity.TemplateStatus;
import java.time.Instant;
import java.util.UUID;

public class TemplateDetailDto {
    private final UUID id;
    private final String code;
    private final String subject;
    private final String bodyHtml;
    private final String bodyText;
    private final TemplateStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TemplateDetailDto(TemplateView view) {
        this.id = view.getId();
        this.code = view.getCode();
        this.subject = view.getSubject();
        this.bodyHtml = view.getBodyHtml();
        this.bodyText = view.getBodyText();
        this.status = view.getStatus();
        this.createdAt = view.getCreatedAt();
        this.updatedAt = view.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public String getBodyText() {
        return bodyText;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
