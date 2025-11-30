package com.example.notifi.api.web.admin.notification.dto;

import com.example.notifi.api.web.shared.notification.dto.CreateNotificationRequest;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AdminCreateNotificationRequest extends CreateNotificationRequest {

  @NotNull private UUID clientId;

  public UUID getClientId() {
    return clientId;
  }

  public void setClientId(UUID clientId) {
    this.clientId = clientId;
  }
}
