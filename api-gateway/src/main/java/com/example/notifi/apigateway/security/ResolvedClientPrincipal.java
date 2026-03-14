package com.example.notifi.apigateway.security;

import java.util.UUID;

public record ResolvedClientPrincipal(UUID clientId, String clientName, int rateLimitPerMin) {}
