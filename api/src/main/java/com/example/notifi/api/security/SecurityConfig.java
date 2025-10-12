package com.example.notifi.api.security;

import com.example.notifi.api.data.repository.ClientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter(ClientRepository clientRepository, ObjectMapper objectMapper) {
        return new ApiKeyAuthFilter(clientRepository, objectMapper);
    }

    @Bean
    public RateLimiter rateLimiter() {
        return new InMemoryRateLimiter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, RequestIdFilter requestIdFilter, ApiKeyAuthFilter apiKeyAuthFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/actuator/health",
                                                "/actuator/info")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiKeyAuthFilter, RequestIdFilter.class);
        return http.build();
    }

}
