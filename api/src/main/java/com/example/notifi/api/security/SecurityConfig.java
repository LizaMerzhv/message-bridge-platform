package com.example.notifi.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final String adminUsername;
  private final String adminPassword;

  public SecurityConfig(
      @Value("${notifi.admin.username:admin}") String adminUsername,
      @Value("${notifi.admin.password:changeit}") String adminPassword) {
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
  }

  @Bean
  public RequestIdFilter requestIdFilter() {
    return new RequestIdFilter();
  }

  @Bean
  public ApiKeyResolverClient apiKeyResolverClient(
      RestClient.Builder restClientBuilder,
      @Value("${notifi.security-service.base-url:http://localhost:8083}") String securityServiceBaseUrl) {
    RestClient restClient = restClientBuilder.baseUrl(securityServiceBaseUrl).build();
    return new SecurityServiceApiKeyResolverClient(restClient);
  }

  @Bean
  public ApiKeyAuthFilter apiKeyAuthFilter(
      ApiKeyResolverClient apiKeyResolverClient, ObjectMapper objectMapper) {
    return new ApiKeyAuthFilter(apiKeyResolverClient, objectMapper);
  }

  @Bean
  public RateLimiter rateLimiter() {
    return new InMemoryRateLimiter();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, RequestIdFilter requestIdFilter, ApiKeyAuthFilter apiKeyAuthFilter)
      throws Exception {

    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/internal/**")
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .requestMatchers("/logout")
                    .permitAll()
                    .requestMatchers("/admin/ui/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .httpBasic(Customizer.withDefaults())
        .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(apiKeyAuthFilter, RequestIdFilter.class)
        .logout(
            logout ->
                logout
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                    .clearAuthentication(true)
                    .logoutSuccessHandler(
                        (request, response, authentication) -> {
                          response.setStatus(HttpStatus.UNAUTHORIZED.value());
                          response.setHeader("WWW-Authenticate", "Basic realm=\"Notifi Admin\"");
                        }));

    return http.build();
  }

  @Bean
  public UserDetailsService users() {
    PasswordEncoder encoder = passwordEncoder();
    return new InMemoryUserDetailsManager(
        User.withUsername(adminUsername)
            .password(encoder.encode(adminPassword))
            .roles("ADMIN")
            .build());
  }
}
