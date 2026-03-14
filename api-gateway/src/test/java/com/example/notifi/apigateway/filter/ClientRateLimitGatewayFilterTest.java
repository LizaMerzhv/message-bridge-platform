package com.example.notifi.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class ClientRateLimitGatewayFilterTest {

  @Test
  void shouldReturn429WithRateLimitHeadersWhenLimitExceeded() {
    ClientRateLimiter limiter = Mockito.mock(ClientRateLimiter.class);
    GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
    when(limiter.checkAndConsume(eq("client-1"), eq(2)))
        .thenReturn(Mono.just(new ClientRateLimitDecision(false, 0, 17)));

    ClientRateLimitGatewayFilter filter = new ClientRateLimitGatewayFilter(limiter);
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/notifications")
                .header(ApiKeyGatewayFilter.X_CLIENT_ID, "client-1")
                .header(ApiKeyGatewayFilter.X_RATE_LIMIT_PER_MIN, "2")
                .build());

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("2");
    assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
        .isEqualTo("0");
    assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("17");
    verify(chain, never()).filter(exchange);
  }

  @Test
  void shouldSkipRateLimitForNonApiV1Path() {
    ClientRateLimiter limiter = Mockito.mock(ClientRateLimiter.class);
    GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
    when(chain.filter(Mockito.any())).thenReturn(Mono.empty());

    ClientRateLimitGatewayFilter filter = new ClientRateLimitGatewayFilter(limiter);
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

    filter.filter(exchange, chain).block();

    verify(chain).filter(exchange);
    verify(limiter, never()).checkAndConsume(Mockito.anyString(), anyInt());
  }
}
