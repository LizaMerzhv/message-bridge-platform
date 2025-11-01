package com.example.notifi.api.web.notification.dto;

import java.time.Instant;
import java.util.UUID;

public class CreateNotificationResponse {
    private UUID id;

    /** CREATED | QUEUED | SENT | FAILED */
    private String status;

    private Instant sendAtEffective;

    public CreateNotificationResponse() {}

    public CreateNotificationResponse(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }

    public String getStatus() { return status; }

    public Instant getSendAtEffective() { return sendAtEffective; }

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
