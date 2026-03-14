package com.example.notifi.api.security;

import java.util.UUID;

public record ClientPrincipal(UUID clientId, String name, int rateLimitPerMinute) {}
