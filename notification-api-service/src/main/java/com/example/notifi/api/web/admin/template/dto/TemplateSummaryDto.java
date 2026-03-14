package com.example.notifi.api.web.admin.template.dto;

import java.time.Instant;
import java.util.UUID;

public class TemplateSummaryDto {

  private UUID id;
  private String code;
  private String subject;
  private String status;
  private Instant createdAt;
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public TemplateSummaryDto setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getCode() {
    return code;
  }

  public TemplateSummaryDto setCode(String code) {
    this.code = code;
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public TemplateSummaryDto setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public TemplateSummaryDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public TemplateSummaryDto setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public TemplateSummaryDto setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
