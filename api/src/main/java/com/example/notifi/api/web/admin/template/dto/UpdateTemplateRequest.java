package com.example.notifi.api.web.admin.template.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public class UpdateTemplateRequest {

    @Size(max = 262_144)
    private String bodyHtml;

    @Size(max = 262_144)
    private String bodyText;

    private String subject;

    private String status;

    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAtLeastOneField() {
        return StringUtils.hasText(subject)
            || bodyHtml != null
            || bodyText != null
            || StringUtils.hasText(status);
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
