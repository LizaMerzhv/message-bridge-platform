package com.example.notifi.apigateway.filter;

public record ClientRateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {}
