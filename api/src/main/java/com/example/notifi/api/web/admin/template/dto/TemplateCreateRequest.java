package com.example.notifi.api.web.admin.template.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TemplateCreateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank private String subject;

  @Size(max = 262_144)
  private String bodyHtml;

  @Size(max = 262_144)
  private String bodyText;

  private String status;

  @AssertTrue(message = "Either bodyHtml or bodyText must be provided")
  public boolean hasBody() {
    return bodyHtml != null || bodyText != null;
  }

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
