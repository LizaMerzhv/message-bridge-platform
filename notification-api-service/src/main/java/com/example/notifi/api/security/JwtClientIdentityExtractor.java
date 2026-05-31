package com.example.notifi.api.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtClientIdentityExtractor {

    public String extractClientIdentity(Jwt jwt) {
        return firstNonBlank(
            jwt.getClaimAsString("azp"), jwt.getClaimAsString("client_id"), jwt.getSubject());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
