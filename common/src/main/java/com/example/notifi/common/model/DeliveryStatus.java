package com.example.notifi.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeliveryStatus {
    PENDING, SENT, FAILED;

    @JsonCreator
    public static DeliveryStatus from(String v) {
        return v == null ? null : DeliveryStatus.valueOf(v.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
