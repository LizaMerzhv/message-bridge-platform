package com.example.notifi.api.security;

import com.example.notifi.common.security.ResolvedClientPrincipal;
import java.util.Optional;

public interface ApiKeyResolverClient {
  Optional<ResolvedClientPrincipal> resolveByApiKey(String apiKey);
}
