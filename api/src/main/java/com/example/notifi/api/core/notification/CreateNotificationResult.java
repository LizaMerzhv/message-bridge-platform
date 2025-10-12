package com.example.notifi.api.core.notification;

import com.example.notifi.api.data.entity.NotificationEntity;

public class CreateNotificationResult {
    private final NotificationEntity entity;
    private final boolean replayed;

    public CreateNotificationResult(NotificationEntity entity, boolean replayed) {
        this.entity = entity;
        this.replayed = replayed;
    }

    public NotificationEntity getEntity() {
        return entity;
    }

    public boolean isReplayed() {
        return replayed;
    }
}
