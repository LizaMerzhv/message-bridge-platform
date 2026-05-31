package com.example.notifi.api.security;

import com.example.notifi.api.data.entity.ClientEntity;
import com.example.notifi.api.data.repository.ClientRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ClientPrincipalJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtClientIdentityExtractor identityExtractor;
    private final ClientRepository clientRepository;

    public ClientPrincipalJwtAuthenticationConverter(
        JwtClientIdentityExtractor identityExtractor, ClientRepository clientRepository) {
        this.identityExtractor = identityExtractor;
        this.clientRepository = clientRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String keycloakClientId = identityExtractor.extractClientIdentity(jwt);
        if (keycloakClientId == null) {
            throw new BadCredentialsException("JWT does not contain azp, client_id, or sub");
        }

        ClientEntity client =
            clientRepository
                .findByKeycloakClientId(keycloakClientId)
                .orElseThrow(
                    () ->
                        new BadCredentialsException("Unknown Keycloak client: " + keycloakClientId));

        return new ClientAuthenticationToken(
            new ClientPrincipal(client.getId(), client.getName(), client.getRateLimitPerMin()));
    }
}
