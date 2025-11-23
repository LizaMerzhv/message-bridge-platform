package com.example.notifi.worker.webhook;

import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.data.entity.NotificationEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpWebhookDispatcher implements WebhookDispatcher {
  private static final Logger log = LoggerFactory.getLogger(HttpWebhookDispatcher.class);
  private static final Duration[] RETRY_BACKOFF =
      new Duration[] {Duration.ofSeconds(5), Duration.ofSeconds(15), Duration.ofSeconds(45)};

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final HmacSigner signer;
  private final WorkerMetrics metrics;
  public HttpWebhookDispatcher(RestClient.Builder builder, ObjectMapper objectMapper, HmacSigner signer, WorkerMetrics metrics) {
    this.restClient = builder.build();
    this.objectMapper = objectMapper;
    this.signer = signer;
    this.metrics = metrics;
  }

  @Override
  public void dispatch(NotificationEntity notification) {
    if (notification.getWebhookUrl() == null || notification.getWebhookUrl().isBlank()) {
      return;
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("notificationId", notification.getId());
    payload.put("status", notification.getStatus().name());
    payload.put("clientId", notification.getClientId());
    payload.put("externalRequestId", notification.getExternalRequestId());
    payload.put("channel", notification.getChannel().name());
    payload.put("traceId", notification.getTraceId());
    try {
      byte[] body = objectMapper.writeValueAsBytes(payload);
      String signature = signer.sign(body, notification.getWebhookSecret());
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.add("X-Signature", "sha256=" + signature);
      sendWithRetry(notification.getWebhookUrl(), new HttpEntity<>(body, headers));
    } catch (Exception ex) {
      log.warn("Failed to serialize webhook payload", ex);
      metrics.incrementWebhookFailed();
    }
  }

  private void sendWithRetry(String url, HttpEntity<byte[]> request) {
    for (int attempt = 0; attempt < RETRY_BACKOFF.length; attempt++) {
      try {
        ResponseEntity<Void> response =
            restClient
                .post()
                .uri(URI.create(url))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request.getBody())
                .header("X-Signature", request.getHeaders().getFirst("X-Signature"))
                .retrieve()
                .toBodilessEntity();
        if (response.getStatusCode().is2xxSuccessful()) {
          metrics.incrementWebhookSent();
          return;
        }
      } catch (Exception ex) {
        log.warn("Webhook attempt {} failed", attempt + 1, ex);
      }
      sleepWithJitter(RETRY_BACKOFF[attempt]);
    }
    metrics.incrementWebhookFailed();
  }

  private void sleepWithJitter(Duration base) {
    long minMillis = Math.round(base.toMillis() * 0.8);
    long maxMillis = Math.round(base.toMillis() * 1.2);
    long jitter = ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1);
    try {
      Thread.sleep(jitter);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}
