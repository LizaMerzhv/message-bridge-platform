package com.example.notifi.adminservice.dto;

import java.time.Instant;
import java.util.UUID;

public class NotificationSummaryDto {
  private UUID id;
  private UUID clientId;
  private String channel;
  private String to;
  private String subject;
  private String templateCode;
  private String status;
  private Instant sendAtEffective;
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public UUID getClientId() {
    return clientId;
  }

  public String getChannel() {
    return channel;
  }

  public String getTo() {
    return to;
  }

  public String getSubject() {
    return subject;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public String getStatus() {
    return status;
  }

  public Instant getSendAtEffective() {
    return sendAtEffective;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
