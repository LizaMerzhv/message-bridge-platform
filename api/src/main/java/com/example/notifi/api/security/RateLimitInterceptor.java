package com.example.notifi.api.security;

import com.example.notifi.api.web.error.ProblemDetails;
import com.example.notifi.api.web.error.Problems;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Clock;
import java.time.Instant;

import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimiter rateLimiter, Clock clock, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {

        Object principalAttr = request.getAttribute(ClientPrincipal.class.getName());
        ClientPrincipal principal =
            principalAttr instanceof ClientPrincipal cp ? cp : SecurityUtils.currentPrincipal();

        if (principal == null) {
            return true;
        }

        RateLimitDecision decision =
            rateLimiter.checkAndConsume(principal.clientId(), principal.rateLimitPerMinute(), now());

        response.setHeader("X-RateLimit-Limit", Integer.toString(principal.rateLimitPerMinute()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(Math.max(decision.remaining(), 0)));

        if (!decision.allowed()) {
            if (decision.retryAfterSeconds() > 0) {
                response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            }
            return writeTooManyRequestsProblem(request, response); // всегда возвращает false
        }

        return true;
    }

    private Instant now() {
        return clock.instant();
    }

    private boolean writeTooManyRequestsProblem(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException {

        ProblemDetails body =
            Problems.tooManyRequests(
                "Rate limit exceeded",
                request.getRequestURI(),
                org.slf4j.MDC.get("traceId")
            );

        response.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
        return false;
    }
}
