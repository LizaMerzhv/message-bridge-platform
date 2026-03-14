package com.example.notifi.securityservice.web.internal;

import com.example.notifi.securityservice.data.repository.ClientRepository;
import com.example.notifi.securityservice.web.internal.dto.ResolvedClientPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/security")
public class ApiKeyResolveController {

  private static final String API_KEY_HEADER = "X-API-Key";

  private final ClientRepository clientRepository;

  public ApiKeyResolveController(ClientRepository clientRepository) {
    this.clientRepository = clientRepository;
  }

  @GetMapping("/resolve")
  public ResolvedClientPrincipal resolveClient(
      @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey) {

    if (apiKey == null || apiKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid API key");
    }

    return clientRepository
        .findByApiKey(apiKey)
        .map(c -> new ResolvedClientPrincipal(c.getId(), c.getName(), c.getRateLimitPerMin()))
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid API key"));
  }
}
