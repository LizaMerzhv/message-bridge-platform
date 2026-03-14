package com.example.notifi.worker.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.example.notifi.worker.data.entity.NotificationEntity;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.model.Channel;
import com.example.notifi.worker.model.NotificationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class HttpWebhookDispatcherTest {

  private RestClient restClient;
  private RestClient.Builder builder;
  private WorkerMetrics metrics;
  private HttpWebhookDispatcher dispatcher;

  @BeforeEach
  void setUp() throws Exception {
    restClient = Mockito.mock(RestClient.class, Mockito.RETURNS_DEEP_STUBS);
    builder = Mockito.mock(RestClient.Builder.class);
    when(builder.build()).thenReturn(restClient);
    metrics = new WorkerMetrics(new SimpleMeterRegistry());
    dispatcher =
        new HttpWebhookDispatcher(
            builder, new ObjectMapper().findAndRegisterModules(), new HmacSigner(), metrics);
    shortenBackoff();
  }

  @Test
  void dispatch_ShouldRecordSuccessOn2xxResponse() {
    when(restClient
            .post()
            .uri(any(URI.class))
            .contentType(eq(MediaType.APPLICATION_JSON))
            .body(any(byte[].class))
            .header(eq("X-Signature"), anyString())
            .retrieve()
            .toBodilessEntity())
        .thenReturn(ResponseEntity.ok().build());

    dispatcher.dispatch(notification());

    assertThat(metrics.registry().get("webhooks_sent_total").counter().count()).isEqualTo(1.0d);
  }

  @Test
  void dispatch_ShouldRetryAndCountFailure() {
    when(restClient
            .post()
            .uri(any(URI.class))
            .contentType(eq(MediaType.APPLICATION_JSON))
            .body(any(byte[].class))
            .header(eq("X-Signature"), anyString())
            .retrieve()
            .toBodilessEntity())
        .thenReturn(
            ResponseEntity.status(500).build(),
            ResponseEntity.status(500).build(),
            ResponseEntity.status(500).build());

    dispatcher.dispatch(notification());

    assertThat(metrics.registry().get("webhooks_failed_total").counter().count()).isEqualTo(1.0d);
  }

  @Test
  void dispatch_ShouldHandleExceptionsFromClient() {
    AtomicInteger attempts = new AtomicInteger();
    when(restClient
            .post()
            .uri(any(URI.class))
            .contentType(eq(MediaType.APPLICATION_JSON))
            .body(any(byte[].class))
            .header(eq("X-Signature"), anyString())
            .retrieve()
            .toBodilessEntity())
        .then(
            invocation -> {
              if (attempts.getAndIncrement() < 2) {
                throw new RuntimeException("boom");
              }
              return ResponseEntity.status(502).build();
            });

    dispatcher.dispatch(notification());

    assertThat(metrics.registry().get("webhooks_failed_total").counter().count()).isEqualTo(1.0d);
  }

  private NotificationEntity notification() {
    NotificationEntity entity = new NotificationEntity();
    entity.setId(UUID.randomUUID());
    entity.setClientId(UUID.randomUUID());
    entity.setExternalRequestId("ext-1");
    entity.setChannel(Channel.EMAIL);
    entity.setStatus(NotificationStatus.SENT);
    entity.setTraceId("trace-1");
    entity.setWebhookUrl("http://localhost/webhook");
    entity.setWebhookSecret("secret");
    entity.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    return entity;
  }

  private void shortenBackoff() throws Exception {
    Field field = HttpWebhookDispatcher.class.getDeclaredField("RETRY_BACKOFF");
    field.setAccessible(true);
    Duration[] backoff = (Duration[]) field.get(null);
    for (int i = 0; i < backoff.length; i++) {
      backoff[i] = Duration.ofMillis(1);
    }
  }
}
