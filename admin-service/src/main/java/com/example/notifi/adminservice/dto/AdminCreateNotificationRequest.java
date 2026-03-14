package com.example.notifi.adminservice.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AdminCreateNotificationRequest {
  private UUID clientId;
  private String channel;
  private String to;
  private String subject;
  private String templateCode;
  private Map<String, Object> variables;
  private Instant sendAt;
  private String externalRequestId;

  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }
  public String getTo() { return to; }
  public void setTo(String to) { this.to = to; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
  public String getTemplateCode() { return templateCode; }
  public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
  public Map<String, Object> getVariables() { return variables; }
  public void setVariables(Map<String, Object> variables) { this.variables = variables; }
  public Instant getSendAt() { return sendAt; }
  public void setSendAt(Instant sendAt) { this.sendAt = sendAt; }
  public String getExternalRequestId() { return externalRequestId; }
  public void setExternalRequestId(String externalRequestId) { this.externalRequestId = externalRequestId; }
}
