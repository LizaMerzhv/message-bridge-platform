package com.example.notifi.api.security;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimiter implements RateLimiter {

    private final Map<UUID, WindowState> state = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision checkAndConsume(UUID clientId, int limitPerMinute, Instant nowUtc) {
        long windowKey = nowUtc.getEpochSecond() / 60;
        WindowState result =
                state.compute(
                        clientId,
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
            return new RateLimitDecision(false, 0, retryAfter);
        }

        int remaining = Math.max(0, limitPerMinute - result.used);
        return new RateLimitDecision(true, remaining, 0);
    }

    private record WindowState(long windowKey, int used) {}
}
