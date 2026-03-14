package com.example.notifi.api.security;

import com.example.notifi.common.security.ResolvedClientPrincipal;
import com.example.notifi.api.web.http.Headers;
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
              .header(Headers.X_API_KEY, apiKey)
              .retrieve()
              .body(ResolvedClientPrincipal.class);
      return Optional.ofNullable(principal);
    } catch (RestClientException ex) {
      return Optional.empty();
    }
  }
}
