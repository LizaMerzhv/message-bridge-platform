package com.example.notifi.api.security;

import java.time.Instant;
import java.util.UUID;

/** Placeholder for future Redis-backed implementation. */
public class RedisRateLimiter implements RateLimiter {
    @Override
    public RateLimitDecision checkAndConsume(UUID clientId, int limitPerMinute, Instant nowUtc) {
        throw new UnsupportedOperationException("Redis rate limiter not implemented yet");
    }
}
