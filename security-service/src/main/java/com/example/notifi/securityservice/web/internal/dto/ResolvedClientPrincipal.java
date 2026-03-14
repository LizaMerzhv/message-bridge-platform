package com.example.notifi.securityservice.web.internal.dto;

import java.util.UUID;

public record ResolvedClientPrincipal(UUID clientId, String clientName, int rateLimitPerMin) {}
