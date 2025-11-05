package com.example.notificationapp.adminui.web.form;

import com.example.notificationapp.adminui.web.validation.SubjectTemplateXor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;

@SubjectTemplateXor
public class NotificationCreateForm {

    private static final DateTimeFormatter DATE_TIME_LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @NotBlank @Email private String to;

    private String subject;

    private String templateCode;

    private String variables;

    private String sendAt;

    @NotBlank
    @Size(max = 64)
    private String externalRequestId;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getSendAt() {
        return sendAt;
    }

    public void setSendAt(String sendAt) {
        this.sendAt = sendAt;
    }

    public String getExternalRequestId() {
        return externalRequestId;
    }

    public void setExternalRequestId(String externalRequestId) {
        this.externalRequestId = externalRequestId;
    }

    public Map<String, Object> toRequest(Map<String, Object> variablesObject, Optional<OffsetDateTime> sendAtUtc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", "email");
        body.put("to", to);
        body.put("externalRequestId", externalRequestId);
        if (StringUtils.hasText(subject)) {
            body.put("subject", subject.trim());
        }
        if (StringUtils.hasText(templateCode)) {
            body.put("templateCode", templateCode.trim());
        }
        if (variablesObject != null && !variablesObject.isEmpty()) {
            body.put("variables", variablesObject);
        }
        sendAtUtc.ifPresent(value -> body.put("sendAt", value));
        return body;
    }

    public Optional<OffsetDateTime> sendAtUtc() {
        if (!StringUtils.hasText(sendAt)) {
            return Optional.empty();
        }
        String trimmed = sendAt.trim();
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(trimmed);
            return Optional.of(parsed.withOffsetSameInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed, DATE_TIME_LOCAL_FORMATTER);
            return Optional.of(localDateTime.atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }
}
