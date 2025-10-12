package com.example.notifi.api.security;

public record RateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {}
