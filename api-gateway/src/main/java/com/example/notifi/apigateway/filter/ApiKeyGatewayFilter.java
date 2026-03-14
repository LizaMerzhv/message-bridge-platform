package com.example.notifi.apigateway.filter;

import com.example.notifi.common.security.ResolvedClientPrincipal;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyGatewayFilter implements GlobalFilter, Ordered {

  public static final String X_API_KEY = "X-API-Key";
  public static final String X_CLIENT_ID = "X-Client-Id";
  public static final String X_CLIENT_NAME = "X-Client-Name";
  public static final String X_RATE_LIMIT_PER_MIN = "X-RateLimit-Per-Min";
  public static final String X_GATEWAY_AUTH = "X-Gateway-Auth";

  private final WebClient webClient;
  private final String gatewaySharedSecret;

  public ApiKeyGatewayFilter(
      WebClient.Builder webClientBuilder,
      @Value("${notifi.security-service.base-url:http://security-service:8080}")
          String securityServiceBaseUrl,
      @Value("${notifi.gateway.shared-secret:notifi-gateway-dev-secret}")
          String gatewaySharedSecret) {
    this.webClient = webClientBuilder.baseUrl(securityServiceBaseUrl).build();
    this.gatewaySharedSecret = gatewaySharedSecret;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (!path.startsWith("/api/v1/")) {
      return chain.filter(exchange);
    }

    String apiKey = exchange.getRequest().getHeaders().getFirst(X_API_KEY);
    if (apiKey == null || apiKey.isBlank()) {
      return writeUnauthorized(exchange, "Missing or invalid API key");
    }

    return webClient
        .get()
        .uri("/internal/security/resolve")
        .header(X_API_KEY, apiKey)
        .retrieve()
        .bodyToMono(ResolvedClientPrincipal.class)
        .flatMap(
            principal -> {
              ServerHttpRequest.Builder requestBuilder =
                  exchange
                      .getRequest()
                      .mutate()
                      .headers(
                          headers -> {
                            headers.remove(X_API_KEY);
                            headers.remove(X_CLIENT_ID);
                            headers.remove(X_CLIENT_NAME);
                            headers.remove(X_RATE_LIMIT_PER_MIN);
                            headers.remove(X_GATEWAY_AUTH);
                            headers.set(X_CLIENT_ID, principal.clientId().toString());
                            headers.set(X_CLIENT_NAME, principal.clientName());
                            headers.set(
                                X_RATE_LIMIT_PER_MIN,
                                Integer.toString(principal.rateLimitPerMin()));
                            if (!gatewaySharedSecret.isBlank()) {
                              headers.set(X_GATEWAY_AUTH, gatewaySharedSecret);
                            }
                          });

              return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
            })
        .onErrorResume(ex -> writeUnauthorized(exchange, "Missing or invalid API key"));
  }

  private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String detail) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] payload =
        ("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"" + detail + "\"}")
            .getBytes(StandardCharsets.UTF_8);
    return exchange
        .getResponse()
        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
