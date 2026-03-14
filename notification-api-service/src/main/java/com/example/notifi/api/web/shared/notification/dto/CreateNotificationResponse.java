package com.example.notifi.api.web.shared.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification creation response")
public class CreateNotificationResponse {
  private UUID id;

  @Schema(description = "Current notification status", example = "QUEUED")
  private String status;

  @Schema(
      description = "Scheduled timestamp applied after validation",
      example = "2024-06-18T10:15:30Z")
  private Instant sendAtEffective;

  public CreateNotificationResponse() {}

  public CreateNotificationResponse(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getStatus() {
    return status;
  }

  public Instant getSendAtEffective() {
    return sendAtEffective;
  }

  public CreateNotificationResponse setId(UUID id) {
    this.id = id;
    return this;
  }

  public CreateNotificationResponse setStatus(String status) {
    this.status = status;
    return this;
  }

  public CreateNotificationResponse setSendAtEffective(Instant sendAtEffective) {
    this.sendAtEffective = sendAtEffective;
    return this;
  }
}
