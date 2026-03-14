package com.example.notifi.common.security;

import java.util.UUID;

public record ResolvedClientPrincipal(UUID clientId, String clientName, int rateLimitPerMin) {}
