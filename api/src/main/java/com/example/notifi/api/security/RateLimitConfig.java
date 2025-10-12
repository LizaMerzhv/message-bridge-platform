package com.example.notifi.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public RateLimitConfig(RateLimiter rateLimiter, Clock clock, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(rateLimiter, clock, objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor()).addPathPatterns("/api/**");
    }
}
