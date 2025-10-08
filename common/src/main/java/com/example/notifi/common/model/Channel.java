package com.example.notifi.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Channel {
    EMAIL;

    @JsonCreator
    public static Channel from(String v) {
        return v == null ? null : Channel.valueOf(v.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
