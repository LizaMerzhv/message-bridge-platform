package com.example.notifi.worker.consumer;

import com.example.notifi.worker.model.NotificationStatus;
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
  private static final String INTERNAL_AUTH_HEADER = "X-Internal-Auth";

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String internalSharedSecret;

  public NotificationApiClient(
      RestTemplate restTemplate,
      @Value("${worker.api.internal.base-url:http://localhost:8080}") String baseUrl,
      @Value("${worker.api.internal.shared-secret:notifi-internal-dev-secret}")
          String internalSharedSecret) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;
    this.internalSharedSecret = internalSharedSecret;
  }

  public void sendDeliveryResult(
      UUID notificationId,
      NotificationStatus status,
      int attempt,
      Instant occurredAt,
      String errorCode,
      String errorMessage) {

    DeliveryResultRequest body =
        new DeliveryResultRequest(status, attempt, errorCode, errorMessage, occurredAt);

    URI uri =
        UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/internal/notifications/{id}/deliveries")
            .build(notificationId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(INTERNAL_AUTH_HEADER, internalSharedSecret);

    ResponseEntity<Void> response =
        restTemplate.postForEntity(uri, new HttpEntity<>(body, headers), Void.class);

    if (response.getStatusCode().is2xxSuccessful()) {
      log.debug("Reported delivery {} as {} to API", notificationId, status);
    } else {
      log.warn(
          "API callback for notification {} responded with status {}",
          notificationId,
          response.getStatusCode());
    }
  }

  public record DeliveryResultRequest(
      NotificationStatus status,
      int attempt,
      String errorCode,
      String errorMessage,
      Instant occurredAt) {}
}
