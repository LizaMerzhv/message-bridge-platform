package com.example.notifi.api.data.entity;

import com.example.notifi.common.model.DeliveryStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery")
public class DeliveryEntity {

  @Id private UUID id;

  @Column(name = "\"notificationId\"", nullable = false)
  private UUID notificationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  @Column(nullable = false)
  private int attempt;

  private String channel;

  @Column(name = "\"to\"", length = 254)
  private String to;

  private String subject;

  @Column(name = "\"errorCode\"")
  private String errorCode;

  @Column(name = "\"errorMessage\"")
  private String errorMessage;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"lastAttemptAt\"")
  private Instant lastAttemptAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getNotificationId() {
    return notificationId;
  }

  public void setNotificationId(UUID notificationId) {
    this.notificationId = notificationId;
  }

  public DeliveryStatus getStatus() {
    return status;
  }

  public void setStatus(DeliveryStatus status) {
    this.status = status;
  }

  public int getAttempt() {
    return attempt;
  }

  public void setAttempt(int attempt) {
    this.attempt = attempt;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  public void setLastAttemptAt(Instant lastAttemptAt) {
    this.lastAttemptAt = lastAttemptAt;
  }
}
