package com.example.notifi.adminservice.dto;

import java.time.Instant;

public class TemplateSummaryDto {
  private String code;
  private String subject;
  private String status;
  private Instant createdAt;
  private Instant updatedAt;

  public String getCode() {
    return code;
  }

  public String getSubject() {
    return subject;
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
