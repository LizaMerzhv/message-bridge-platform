package com.example.notifi.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtClientIdentityExtractorTest {

  private final JwtClientIdentityExtractor extractor = new JwtClientIdentityExtractor();

  @Test
  void prefersAzpThenClientIdThenSubject() {
    assertThat(
            extractor.extractClientIdentity(
                jwt(Map.of("azp", "azp-client", "client_id", "client", "sub", "subject"))))
        .isEqualTo("azp-client");
    assertThat(
            extractor.extractClientIdentity(jwt(Map.of("client_id", "client", "sub", "subject"))))
        .isEqualTo("client");
    assertThat(extractor.extractClientIdentity(jwt(Map.of("sub", "subject")))).isEqualTo("subject");
  }

  @Test
  void returnsNullWhenNoSupportedClaimExists() {
    assertThat(extractor.extractClientIdentity(jwt(Map.of("iss", "issuer")))).isNull();
  }

  private Jwt jwt(Map<String, Object> claims) {
    return new Jwt(
        "token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
  }
}
