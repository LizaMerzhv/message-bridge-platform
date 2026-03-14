package com.example.notifi.apigateway.filter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InMemoryClientRateLimiter implements ClientRateLimiter {

  private final Map<String, WindowState> state = new ConcurrentHashMap<>();

  @Override
  public Mono<ClientRateLimitDecision> checkAndConsume(String key, int limitPerMinute) {
    Instant nowUtc = Instant.now();
    long windowKey = nowUtc.getEpochSecond() / 60;

    WindowState result =
        state.compute(
            key,
            (id, current) -> {
              if (current == null || current.windowKey != windowKey) {
                return new WindowState(windowKey, 1);
              }
              int next = current.used + 1;
              return new WindowState(windowKey, next);
            });

    if (result.used > limitPerMinute) {
      long nextWindowStart = (windowKey + 1) * 60;
      long retryAfter = Math.max(0, nextWindowStart - nowUtc.getEpochSecond());
      return Mono.just(new ClientRateLimitDecision(false, 0, retryAfter));
    }

    int remaining = Math.max(0, limitPerMinute - result.used);
    return Mono.just(new ClientRateLimitDecision(true, remaining, 0));
  }

  private record WindowState(long windowKey, int used) {}
}
