package com.example.notifi.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

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

  public String metricTag() {
    return name().toLowerCase(Locale.ROOT);
  }
}
