package com.example.notifi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class SendAtWindowBoundaryTest {

  private final SendAtWindowValidator v = new SendAtWindowValidator();

  @Test
  void accepts_null() {
    v.initialize(defaultAnno());
    assertThat(v.isValid(null, null)).isTrue();
  }

  @Test
  void rejects_past() {
    v.initialize(defaultAnno());
    assertThat(v.isValid(Instant.now().minusSeconds(1), null)).isFalse();
  }

  @Test
  void accepts_soon_and_inside_window() {
    v.initialize(defaultAnno());
    assertThat(v.isValid(Instant.now().plusMillis(1), null)).isTrue();
    assertThat(v.isValid(Instant.now().plus(Duration.ofDays(100)), null)).isTrue();
  }

  @Test
  void rejects_beyond_window() {
    v.initialize(defaultAnno());
    assertThat(v.isValid(Instant.now().plus(Duration.ofDays(365)).plusSeconds(1), null)).isFalse();
  }

  private SendAtWindow defaultAnno() {
    return new SendAtWindow() {
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return SendAtWindow.class;
      }

      public String message() {
        return "";
      }

      public Class<?>[] groups() {
        return new Class[0];
      }

      public Class<? extends jakarta.validation.Payload>[] payload() {
        return new Class[0];
      }

      public long maxDays() {
        return 365;
      }
    };
  }
}
