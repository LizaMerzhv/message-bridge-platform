package com.example.notifi.apigateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ClientRateLimitGatewayFilter implements GlobalFilter, Ordered {

  private static final String X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
  private static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
  private static final String RETRY_AFTER = "Retry-After";

  private final ClientRateLimiter clientRateLimiter;
  private final int defaultLimitPerMinute;

  public ClientRateLimitGatewayFilter(
      ClientRateLimiter clientRateLimiter,
      @Value("${notifi.gateway.rate-limit.default-per-minute:60}") int defaultLimitPerMinute) {
    this.clientRateLimiter = clientRateLimiter;
    this.defaultLimitPerMinute = defaultLimitPerMinute;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (!path.startsWith("/api/")) {
      return chain.filter(exchange);
    }

    return exchange
        .getPrincipal()
        .cast(Authentication.class)
        .map(this::clientKeyFromAuthentication)
        .defaultIfEmpty("")
        .flatMap(
            clientKey -> {
              if (clientKey.isBlank()) {
                return chain.filter(exchange);
              }
              return applyRateLimit(exchange, chain, clientKey);
            });
  }

  private Mono<Void> applyRateLimit(
      ServerWebExchange exchange, GatewayFilterChain chain, String clientKey) {
    int safeLimit = Math.max(1, defaultLimitPerMinute);
    return clientRateLimiter
        .checkAndConsume(clientKey, safeLimit)
        .flatMap(
            decision -> {
              exchange
                  .getResponse()
                  .getHeaders()
                  .set(X_RATE_LIMIT_LIMIT, Integer.toString(safeLimit));
              exchange
                  .getResponse()
                  .getHeaders()
                  .set(X_RATE_LIMIT_REMAINING, Integer.toString(Math.max(decision.remaining(), 0)));

              if (decision.allowed()) {
                return chain.filter(exchange);
              }

              if (decision.retryAfterSeconds() > 0) {
                exchange
                    .getResponse()
                    .getHeaders()
                    .set(RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
              }

              exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
              exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
              byte[] payload =
                  "{\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Rate limit exceeded\"}"
                      .getBytes(StandardCharsets.UTF_8);
              return exchange
                  .getResponse()
                  .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
            });
  }

  String clientKeyFromAuthentication(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      Jwt jwt = jwtAuthentication.getToken();
      return firstNonBlank(
          jwt.getClaimAsString("azp"), jwt.getClaimAsString("client_id"), jwt.getSubject());
    }
    return "";
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 20;
  }
}
