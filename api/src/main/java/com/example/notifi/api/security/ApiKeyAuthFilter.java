package com.example.notifi.api.security;

import static com.example.notifi.api.web.error.Problems.unauthorized;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

import com.example.notifi.common.security.ResolvedClientPrincipal;
import com.example.notifi.api.web.error.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-API-Key";

  private final ApiKeyResolverClient apiKeyResolverClient;
  private final ObjectMapper objectMapper;

  public ApiKeyAuthFilter(ApiKeyResolverClient apiKeyResolverClient, ObjectMapper objectMapper) {
    this.apiKeyResolverClient = apiKeyResolverClient;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String apiKey = request.getHeader(HEADER);
    if (apiKey == null || apiKey.isBlank()) {
      writeUnauthorized(request, response, "Missing or invalid API key");
      return;
    }

    final ResolvedClientPrincipal resolvedPrincipal =
        apiKeyResolverClient.resolveByApiKey(apiKey).orElse(null);
    if (resolvedPrincipal == null) {
      writeUnauthorized(request, response, "Missing or invalid API key");
      return;
    }

    final ClientPrincipal principal =
        new ClientPrincipal(
            resolvedPrincipal.clientId(),
            resolvedPrincipal.clientName(),
            resolvedPrincipal.rateLimitPerMin());

    final ClientAuthenticationToken authentication = new ClientAuthenticationToken(principal);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    request.setAttribute(ClientPrincipal.class.getName(), principal);

    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();

    if (path.startsWith("/admin/") || path.startsWith("/actuator/") || path.startsWith("/error")) {
      return true;
    }

    return !path.startsWith("/api/v1/");
  }

  private void writeUnauthorized(
      HttpServletRequest request, HttpServletResponse response, String detail) throws IOException {
    final String traceId = MDC.get("traceId");
    final ProblemDetails body = unauthorized(detail, request.getRequestURI(), traceId);
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
