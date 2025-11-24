package com.example.notifi.api.web.internal.notification;

import com.example.notifi.api.data.entity.NotificationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class NotificationDeliveryUpdateRequest {

    @NotNull
    private NotificationStatus status;

    private String errorMessage;

    @NotNull
    private Instant attemptedAt;

    public NotificationStatus getStatus() { return status; }

    public void setStatus(NotificationStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }

    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getAttemptedAt() { return attemptedAt; }

    public void setAttemptedAt(Instant attemptedAt) { this.attemptedAt = attemptedAt; }
}
