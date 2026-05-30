package com.example.notifi.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class ClientRateLimitGatewayFilterTest {

    @Test
    void shouldReturn429WithRateLimitHeadersWhenLimitExceeded() {
        ClientRateLimiter limiter = Mockito.mock(ClientRateLimiter.class);
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        when(limiter.checkAndConsume(eq("client-1"), eq(60)))
            .thenReturn(Mono.just(new ClientRateLimitDecision(false, 0, 17)));

        ClientRateLimitGatewayFilter filter = new ClientRateLimitGatewayFilter(limiter, 60);
        MockServerHttpResponse response = new MockServerHttpResponse();
        ServerWebExchange exchange = authenticatedExchange("/api/v1/notifications", response);

        filter.filter(exchange, chain).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("60");
        assertThat(response.getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("17");
        verify(chain, never()).filter(exchange);
    }

    @Test
    void shouldSkipRateLimitForNonApiPath() {
        ClientRateLimiter limiter = Mockito.mock(ClientRateLimiter.class);
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        when(chain.filter(Mockito.any())).thenReturn(Mono.empty());

        ClientRateLimitGatewayFilter filter = new ClientRateLimitGatewayFilter(limiter, 60);
        MockServerWebExchange exchange =
            MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(limiter, never()).checkAndConsume(Mockito.anyString(), anyInt());
    }

    private ServerWebExchange authenticatedExchange(String path, MockServerHttpResponse response) {
        ServerWebExchange exchange = Mockito.mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(MockServerHttpRequest.get(path).build());
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.<JwtAuthenticationToken>getPrincipal()).thenReturn(Mono.just(jwtAuth("client-1")));
        return exchange;
    }

    private JwtAuthenticationToken jwtAuth(String clientId) {
        Jwt jwt =
            new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("azp", clientId, "sub", "subject"));
        return new JwtAuthenticationToken(jwt);
    }
}
