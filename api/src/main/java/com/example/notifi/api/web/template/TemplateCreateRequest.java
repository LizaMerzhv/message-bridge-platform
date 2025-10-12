package com.example.notifi.api.web.template;

import com.example.notifi.common.validation.TemplateCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TemplateCreateRequest {

    @NotBlank
    @TemplateCode
    private String code;

    @NotBlank private String subject;

    @Size(max = 262_144)
    private String bodyHtml;

    @Size(max = 262_144)
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
}
