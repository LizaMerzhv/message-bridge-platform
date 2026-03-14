package com.example.notifi.adminservice.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class NotificationDetailDto {
  private UUID id;
  private UUID clientId;
  private String channel;
  private String to;
  private String status;
  private String subject;
  private String templateCode;
  private Map<String, Object> variables;
  private String externalRequestId;
  private Instant sendAt;
  private Instant sendAtEffective;
  private Instant createdAt;
  private Instant updatedAt;

  public UUID getId() { return id; }
  public UUID getClientId() { return clientId; }
  public String getChannel() { return channel; }
  public String getTo() { return to; }
  public String getStatus() { return status; }
  public String getSubject() { return subject; }
  public String getTemplateCode() { return templateCode; }
  public Map<String, Object> getVariables() { return variables; }
  public String getExternalRequestId() { return externalRequestId; }
  public Instant getSendAt() { return sendAt; }
  public Instant getSendAtEffective() { return sendAtEffective; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
