package com.example.notifi.api.core.notification;

import com.example.notifi.api.data.entity.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public class NotificationFilter {
  private NotificationStatus status;
  private Instant createdFrom;
  private Instant createdTo;
  private UUID clientId;

  public NotificationStatus getStatus() {
    return status;
  }

  public void setStatus(NotificationStatus status) {
    this.status = status;
  }

  public Instant getCreatedFrom() {
    return createdFrom;
  }

  public void setCreatedFrom(Instant createdFrom) {
    this.createdFrom = createdFrom;
  }

  public Instant getCreatedTo() {
    return createdTo;
  }

  public void setCreatedTo(Instant createdTo) {
    this.createdTo = createdTo;
  }

  public UUID getClientId() {
    return clientId;
  }

  public void setClientId(UUID clientId) {
    this.clientId = clientId;
  }
}
