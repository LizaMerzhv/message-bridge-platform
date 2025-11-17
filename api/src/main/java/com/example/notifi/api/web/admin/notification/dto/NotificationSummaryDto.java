package com.example.notifi.api.web.admin.notification.dto;

import java.time.Instant;
import java.util.UUID;

public class NotificationSummaryDto {
    private UUID id;
    private UUID clientId;
    private String to;
    private String status;
    private Instant sendAt;
    private Instant createdAt;
    private int attempts;

    public UUID getId() {
        return id;
    }

    public NotificationSummaryDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getClientId() {
        return clientId;
    }

    public NotificationSummaryDto setClientId(UUID clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getTo() {
        return to;
    }

    public NotificationSummaryDto setTo(String to) {
        this.to = to;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public NotificationSummaryDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public Instant getSendAt() {
        return sendAt;
    }

    public NotificationSummaryDto setSendAt(Instant sendAt) {
        this.sendAt = sendAt;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public NotificationSummaryDto setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public int getAttempts() {
        return attempts;
    }

    public NotificationSummaryDto setAttempts(int attempts) {
        this.attempts = attempts;
        return this;
    }
}
