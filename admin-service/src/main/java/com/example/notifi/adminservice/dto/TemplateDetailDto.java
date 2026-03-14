package com.example.notifi.adminservice.dto;

import java.time.Instant;

public class TemplateDetailDto {
  private String code;
  private String subject;
  private String bodyHtml;
  private String bodyText;
  private String status;
  private Instant createdAt;
  private Instant updatedAt;

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

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
