package com.example.notificationapp.adminui.config;

import com.example.notificationapp.adminui.api.ApiProblemResponseErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    @Bean
    RestTemplate restTemplate(
            RestTemplateBuilder builder, AdminUiProperties properties, ObjectMapper objectMapper) {
        return builder
                .rootUri(properties.baseUrl())
                .basicAuthentication(properties.username(), properties.password())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()))
                .errorHandler(new ApiProblemResponseErrorHandler(objectMapper))
                .build();
    }
}
