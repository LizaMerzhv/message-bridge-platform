package com.example.notifi.api.security;

import java.util.Collections;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class ClientAuthenticationToken extends AbstractAuthenticationToken {

    private final ClientPrincipal principal;

    public ClientAuthenticationToken(ClientPrincipal principal) {
        super(Collections.emptyList());
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public ClientPrincipal getPrincipal() {
        return principal;
    }
}
