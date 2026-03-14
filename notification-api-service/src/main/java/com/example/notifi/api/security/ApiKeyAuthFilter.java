package com.example.notifi.api.security;

import static com.example.notifi.api.web.error.Problems.unauthorized;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

import com.example.notifi.api.web.error.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

  public static final String CLIENT_ID_HEADER = "X-Client-Id";
  public static final String CLIENT_NAME_HEADER = "X-Client-Name";
  public static final String RATE_LIMIT_HEADER = "X-RateLimit-Per-Min";
  public static final String GATEWAY_AUTH_HEADER = "X-Gateway-Auth";

  private final ObjectMapper objectMapper;
  private final String gatewaySharedSecret;

  public ApiKeyAuthFilter(ObjectMapper objectMapper, String gatewaySharedSecret) {
    this.objectMapper = objectMapper;
    this.gatewaySharedSecret = gatewaySharedSecret;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    ClientPrincipal principal = principalFromGatewayHeaders(request);
    if (principal == null) {
      writeUnauthorized(request, response, "Missing or invalid gateway authentication headers");
      return;
    }

    ClientAuthenticationToken authentication = new ClientAuthenticationToken(principal);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    request.setAttribute(ClientPrincipal.class.getName(), principal);

    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private ClientPrincipal principalFromGatewayHeaders(HttpServletRequest request) {
    String clientId = request.getHeader(CLIENT_ID_HEADER);
    String clientName = request.getHeader(CLIENT_NAME_HEADER);
    String rateLimit = request.getHeader(RATE_LIMIT_HEADER);

    if (clientId == null
        || clientId.isBlank()
        || clientName == null
        || clientName.isBlank()
        || rateLimit == null
        || rateLimit.isBlank()) {
      return null;
    }

    if (gatewaySharedSecret.isBlank()) {
      return null;
    }

    String requestSecret = request.getHeader(GATEWAY_AUTH_HEADER);
    if (!gatewaySharedSecret.equals(requestSecret)) {
      return null;
    }

    try {
      return new ClientPrincipal(
          UUID.fromString(clientId), clientName, Integer.parseInt(rateLimit));
    } catch (IllegalArgumentException ex) {
      return null;
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
    String traceId = MDC.get("traceId");
    ProblemDetails body = unauthorized(detail, request.getRequestURI(), traceId);
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
