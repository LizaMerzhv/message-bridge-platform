package com.example.notifi.common.dto;

import java.util.UUID;

public class CreateNotificationResponse {
    private UUID id;

    public CreateNotificationResponse() {}

    public CreateNotificationResponse(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
}
