package com.example.notifi.apigateway.filter;

import reactor.core.publisher.Mono;

public interface ClientRateLimiter {
  Mono<ClientRateLimitDecision> checkAndConsume(String key, int limitPerMinute);
}
