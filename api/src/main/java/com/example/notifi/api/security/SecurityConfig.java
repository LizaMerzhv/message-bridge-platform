package com.example.notifi.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  private final String gatewaySharedSecret;
  private final String internalSharedSecret;

  public SecurityConfig(
      @Value("${notifi.admin.username:admin}") String adminUsername,
      @Value("${notifi.admin.password:changeit}") String adminPassword,
      @Value("${notifi.gateway.shared-secret:notifi-gateway-dev-secret}") String gatewaySharedSecret,
      @Value("${notifi.internal.shared-secret:notifi-internal-dev-secret}")
          String internalSharedSecret) {
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
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
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
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
        .addFilterAfter(internalServiceAuthFilter, ApiKeyAuthFilter.class)
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
