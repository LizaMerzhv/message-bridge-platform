package com.example.notifi.api.security;

import com.example.notifi.common.security.ResolvedClientPrincipal;
import java.util.Optional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class SecurityServiceApiKeyResolverClient implements ApiKeyResolverClient {

  private final RestClient restClient;

  public SecurityServiceApiKeyResolverClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public Optional<ResolvedClientPrincipal> resolveByApiKey(String apiKey) {
    try {
      ResolvedClientPrincipal principal =
          restClient
              .get()
              .uri("/internal/security/resolve")
              .header(ApiKeyAuthFilter.HEADER, apiKey)
              .retrieve()
              .body(ResolvedClientPrincipal.class);
      return Optional.ofNullable(principal);
    } catch (RestClientException ex) {
      return Optional.empty();
    }
  }
}
