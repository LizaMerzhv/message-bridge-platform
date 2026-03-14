package com.example.notifi.apigateway.filter;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestIdGatewayFilter implements GlobalFilter, Ordered {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    List<String> requestIds = exchange.getRequest().getHeaders().getOrEmpty(REQUEST_ID_HEADER);
    String requestId = requestIds.isEmpty() ? UUID.randomUUID().toString() : requestIds.get(0);

    ServerWebExchange mutatedExchange =
        exchange
            .mutate()
            .request(
                exchange
                    .getRequest()
                    .mutate()
                    .headers(headers -> headers.set(REQUEST_ID_HEADER, requestId))
                    .build())
            .build();

    mutatedExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
    return chain.filter(mutatedExchange);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
