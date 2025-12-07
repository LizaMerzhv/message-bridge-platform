package com.example.notifi.api.web.admin.template.dto;

import java.time.Instant;
import java.util.UUID;

public class TemplateDetailDto {

    private UUID id;
    private String code;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public TemplateDetailDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public TemplateDetailDto setCode(String code) {
        this.code = code;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public TemplateDetailDto setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public TemplateDetailDto setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
        return this;
    }

    public String getBodyText() {
        return bodyText;
    }

    public TemplateDetailDto setBodyText(String bodyText) {
        this.bodyText = bodyText;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public TemplateDetailDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public TemplateDetailDto setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public TemplateDetailDto setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
