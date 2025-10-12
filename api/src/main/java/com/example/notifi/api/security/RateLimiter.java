package com.example.notifi.api.security;

import java.time.Instant;
import java.util.UUID;

public interface RateLimiter {
    RateLimitDecision checkAndConsume(UUID clientId, int limitPerMinute, Instant nowUtc);
}
