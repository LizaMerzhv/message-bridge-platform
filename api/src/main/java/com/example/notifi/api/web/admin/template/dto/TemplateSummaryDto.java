package com.example.notifi.api.web.admin.template.dto;

import com.example.notifi.api.core.template.TemplateView;
import com.example.notifi.api.data.entity.TemplateStatus;
import java.time.Instant;
import java.util.UUID;

public class TemplateSummaryDto {
    private UUID id;
    private String code;
    private String subject;
    private TemplateStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public TemplateSummaryDto() {
        // needed for Jackson
    }

    public TemplateSummaryDto(TemplateView view) {
        this.id = view.getId();
        this.code = view.getCode();
        this.subject = view.getSubject();
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
