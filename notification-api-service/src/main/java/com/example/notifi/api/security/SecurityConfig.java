package com.example.notifi.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final String gatewaySharedSecret;
  private final String internalSharedSecret;

  public SecurityConfig(
      @Value("${notifi.gateway.shared-secret:notifi-gateway-dev-secret}")
          String gatewaySharedSecret,
      @Value("${notifi.internal.shared-secret:notifi-internal-dev-secret}")
          String internalSharedSecret) {
    this.gatewaySharedSecret = gatewaySharedSecret;
    this.internalSharedSecret = internalSharedSecret;
  }

  @Bean
  public RequestIdFilter requestIdFilter() {
    return new RequestIdFilter();
  }

  @Bean
  public ApiKeyAuthFilter apiKeyAuthFilter(ObjectMapper objectMapper) {
    return new ApiKeyAuthFilter(objectMapper, gatewaySharedSecret);
  }

  @Bean
  public InternalServiceAuthFilter internalServiceAuthFilter(ObjectMapper objectMapper) {
    return new InternalServiceAuthFilter(objectMapper, internalSharedSecret);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      RequestIdFilter requestIdFilter,
      ApiKeyAuthFilter apiKeyAuthFilter,
      InternalServiceAuthFilter internalServiceAuthFilter)
      throws Exception {

    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/internal/**")
                    .authenticated()
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .httpBasic(Customizer.withDefaults())
        .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(apiKeyAuthFilter, RequestIdFilter.class)
        .addFilterAfter(internalServiceAuthFilter, ApiKeyAuthFilter.class);

    return http.build();
  }
}
