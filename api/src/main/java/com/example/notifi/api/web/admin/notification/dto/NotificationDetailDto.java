package com.example.notifi.api.web.admin.notification.dto;

import java.time.Instant;
import java.util.List;
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
  private int attempts;
  private List<DeliveryAttemptDto> deliveries;

  public UUID getId() {
    return id;
  }

  public NotificationDetailDto setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getClientId() {
    return clientId;
  }

  public NotificationDetailDto setClientId(UUID clientId) {
    this.clientId = clientId;
    return this;
  }

  public String getChannel() {
    return channel;
  }

  public NotificationDetailDto setChannel(String channel) {
    this.channel = channel;
    return this;
  }

  public String getTo() {
    return to;
  }

  public NotificationDetailDto setTo(String to) {
    this.to = to;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public NotificationDetailDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public NotificationDetailDto setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public NotificationDetailDto setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
    return this;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  public NotificationDetailDto setVariables(Map<String, Object> variables) {
    this.variables = variables;
    return this;
  }

  public String getExternalRequestId() {
    return externalRequestId;
  }

  public NotificationDetailDto setExternalRequestId(String externalRequestId) {
    this.externalRequestId = externalRequestId;
    return this;
  }

  public Instant getSendAt() {
    return sendAt;
  }

  public NotificationDetailDto setSendAt(Instant sendAt) {
    this.sendAt = sendAt;
    return this;
  }

  public Instant getSendAtEffective() {
    return sendAtEffective;
  }

  public NotificationDetailDto setSendAtEffective(Instant sendAtEffective) {
    this.sendAtEffective = sendAtEffective;
    return this;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public NotificationDetailDto setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public NotificationDetailDto setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public int getAttempts() {
    return attempts;
  }

  public NotificationDetailDto setAttempts(int attempts) {
    this.attempts = attempts;
    return this;
  }

  public List<DeliveryAttemptDto> getDeliveries() {
    return deliveries;
  }

  public NotificationDetailDto setDeliveries(List<DeliveryAttemptDto> deliveries) {
    this.deliveries = deliveries;
    return this;
  }
}
