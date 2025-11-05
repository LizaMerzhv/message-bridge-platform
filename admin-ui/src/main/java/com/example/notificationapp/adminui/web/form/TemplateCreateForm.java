package com.example.notificationapp.adminui.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateCreateForm {

    @NotBlank
    @Size(max = 128)
    private String code;

    @NotBlank
    private String subject;

    @NotBlank
    private String bodyHtml;

    @NotBlank
    private String bodyText;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public Map<String, Object> toRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("subject", subject);
        body.put("bodyHtml", bodyHtml);
        body.put("bodyText", bodyText);
        body.put("status", "ACTIVE");
        return body;
    }
}
