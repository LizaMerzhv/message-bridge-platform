package com.example.notifi.api.core.template.exception;

import java.util.UUID;

public class TemplateNotFoundException extends RuntimeException {
    private final UUID templateId;

    public TemplateNotFoundException(UUID templateId) {
        super("Template not found: " + templateId);
        this.templateId = templateId;
    }

    public UUID getTemplateId() {
        return templateId;
    }
}
