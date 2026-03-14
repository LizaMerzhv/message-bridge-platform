package com.example.notifi.api.data.repository;

import com.example.notifi.api.model.DeliveryStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "notification_delivery_attempt",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_notification_delivery_attempt",
          columnNames = {"notificationId", "attempt"})
    })
public class NotificationDeliveryAttemptEntity {

  @Id private UUID id;

  @Column(name = "\"notificationId\"", nullable = false)
  private UUID notificationId;

  @Column(nullable = false)
  private int attempt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  @Column(name = "\"errorCode\"")
  private String errorCode;

  @Column(name = "\"errorMessage\"")
  private String errorMessage;

  @Column(name = "\"occurredAt\"", nullable = false)
  private Instant occurredAt;

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

  public int getAttempt() {
    return attempt;
  }

  public void setAttempt(int attempt) {
    this.attempt = attempt;
  }

  public DeliveryStatus getStatus() {
    return status;
  }

  public void setStatus(DeliveryStatus status) {
    this.status = status;
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

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
