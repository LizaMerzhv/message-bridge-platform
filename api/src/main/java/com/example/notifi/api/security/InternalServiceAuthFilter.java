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
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalServiceAuthFilter extends OncePerRequestFilter {

  public static final String INTERNAL_AUTH_HEADER = "X-Internal-Auth";

  private final ObjectMapper objectMapper;
  private final String internalSharedSecret;

  public InternalServiceAuthFilter(ObjectMapper objectMapper, String internalSharedSecret) {
    this.objectMapper = objectMapper;
    this.internalSharedSecret = internalSharedSecret;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestSecret = request.getHeader(INTERNAL_AUTH_HEADER);
    if (internalSharedSecret.isBlank() || !internalSharedSecret.equals(requestSecret)) {
      writeUnauthorized(request, response, "Missing or invalid internal service authentication");
      return;
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/internal/");
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
