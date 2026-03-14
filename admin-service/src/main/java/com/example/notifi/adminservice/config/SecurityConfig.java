package com.example.notifi.adminservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health").permitAll().anyRequest().hasRole("ADMIN"))
        .formLogin(Customizer.withDefaults())
        .logout(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public UserDetailsService users(
      @Value("${notifi.admin.username:admin}") String username,
      @Value("${notifi.admin.password:admin}") String password,
      PasswordEncoder passwordEncoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password(passwordEncoder.encode(password))
            .roles("ADMIN")
            .build());
  }
}
