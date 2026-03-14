package com.example.notifi.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestIdFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Request-Id";
  public static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(HEADER_NAME);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    MDC.put(MDC_KEY, requestId);
    try {
      response.setHeader(HEADER_NAME, requestId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
