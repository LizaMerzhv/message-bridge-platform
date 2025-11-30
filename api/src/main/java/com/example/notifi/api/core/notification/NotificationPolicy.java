package com.example.notifi.api.core.notification;

import com.example.notifi.api.core.notification.exceptions.SendAtWindowException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class NotificationPolicy {

  private static final Duration PAST_TOLERANCE = Duration.ofMinutes(5);
  private static final Duration FUTURE_LIMIT = Duration.ofDays(365);

  public void validateSendAt(Instant sendAt, Clock clock) {
    if (sendAt == null) {
      return;
    }
    Instant now = clock.instant();
    Instant min = now.minus(PAST_TOLERANCE);
    Instant max = now.plus(FUTURE_LIMIT);
    if (sendAt.isBefore(min) || sendAt.isAfter(max)) {
      throw new SendAtWindowException(sendAt, min, max);
    }
  }
}
