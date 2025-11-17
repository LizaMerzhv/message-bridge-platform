package com.example.notificationapp.adminui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notifi.api")
public record AdminUiProperties(
        String baseUrl,
        String username,
        String password,
        String clientId,
        @DefaultValue("3000") int connectTimeoutMs,
        @DefaultValue("7000") int readTimeoutMs) {}
