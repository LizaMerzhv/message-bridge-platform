package com.example.notifi.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationStatus {
    CREATED, QUEUED, SENT, FAILED;

    @JsonCreator
    public static NotificationStatus from(String v) {
        return v == null ? null : NotificationStatus.valueOf(v.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
