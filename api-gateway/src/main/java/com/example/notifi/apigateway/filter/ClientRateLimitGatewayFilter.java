package com.example.notifi.apigateway.filter;

import com.example.notifi.common.security.ResolvedClientPrincipal;
import java.nio.charset.StandardCharsets;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ClientRateLimitGatewayFilter implements GlobalFilter, Ordered {

  private static final String X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
  private static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
  private static final String RETRY_AFTER = "Retry-After";

  private final ClientRateLimiter clientRateLimiter;

  public ClientRateLimitGatewayFilter(ClientRateLimiter clientRateLimiter) {
    this.clientRateLimiter = clientRateLimiter;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (!path.startsWith("/api/v1/")) {
      return chain.filter(exchange);
    }

    ResolvedClientPrincipal principal =
        exchange.getAttribute(ApiKeyGatewayFilter.RESOLVED_CLIENT_PRINCIPAL_ATTR);

    String clientKey =
        principal != null
            ? principal.clientId().toString()
            : exchange.getRequest().getHeaders().getFirst(ApiKeyGatewayFilter.X_CLIENT_ID);
    int limitPerMinute =
        principal != null
            ? principal.rateLimitPerMin()
            : parseLimitPerMinute(
                exchange.getRequest().getHeaders().getFirst(ApiKeyGatewayFilter.X_RATE_LIMIT_PER_MIN));

    if (clientKey == null || clientKey.isBlank()) {
      return chain.filter(exchange);
    }

    int safeLimit = Math.max(1, limitPerMinute);
    return clientRateLimiter
        .checkAndConsume(clientKey, safeLimit)
        .flatMap(
            decision -> {
              exchange.getResponse().getHeaders().set(X_RATE_LIMIT_LIMIT, Integer.toString(safeLimit));
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

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 20;
  }

  private int parseLimitPerMinute(String rawLimit) {
    try {
      return rawLimit == null ? 1 : Integer.parseInt(rawLimit);
    } catch (NumberFormatException ex) {
      return 1;
    }
  }
}
