package com.example.notifi.worker.consumer;

import com.example.notifi.common.model.NotificationStatus;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NotificationApiClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationApiClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificationApiClient(RestTemplate restTemplate, @Value("${API_INTERNAL_BASE_URL:http://localhost:8080}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void sendDeliveryResult(UUID notificationId, NotificationStatus status, Instant attemptedAt, String errorMessage) {
        DeliveryResultRequest body = new DeliveryResultRequest(status, errorMessage, attemptedAt);
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/internal/notifications/{id}/deliveries")
            .build(notificationId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response = restTemplate.postForEntity(uri, new HttpEntity<>(body, headers), Void.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            log.debug("Reported delivery {} as {} to API", notificationId, status);
        } else {
            log.warn("API callback for notification {} responded with status {}", notificationId, response.getStatusCode());
        }
    }

    public record DeliveryResultRequest(NotificationStatus status, String errorMessage, Instant attemptedAt) {}
}
