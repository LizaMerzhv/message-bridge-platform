package com.example.notifi.worker.metrics;

import com.example.notifi.worker.model.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class WorkerMetrics {
  private final MeterRegistry registry;
  private final Counter notificationsQueued;
  private final Map<Channel, Counter> deliveriesSent;
  private final Map<Channel, Counter> deliveriesFailed;
  private final Counter webhooksSent;
  private final Counter webhooksFailed;
  private final Timer sendLatency;
  private final Timer sendFailureLatency;

  public WorkerMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.notificationsQueued = registry.counter("notifications_queued_total");
    this.deliveriesSent = new ConcurrentHashMap<>();
    this.deliveriesFailed = new ConcurrentHashMap<>();
    this.webhooksSent = registry.counter("webhooks_sent_total");
    this.webhooksFailed = registry.counter("webhooks_failed_total");
    this.sendLatency =
        Timer.builder("send_latency_seconds")
            .publishPercentiles()
            .sla(
                Duration.ofMillis(100),
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30))
            .register(registry);
    this.sendFailureLatency =
        Timer.builder("send_failure_seconds")
            .publishPercentiles()
            .sla(
                Duration.ofMillis(100),
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30))
            .register(registry);
  }

  private static String tag(Channel c) {
    return switch (c) {
      case EMAIL -> "email";
      default -> c.name().toLowerCase(Locale.ROOT);
    };
  }

  public MeterRegistry registry() {
    return registry;
  }

  public void incrementNotificationsQueued() {
    notificationsQueued.increment();
  }

  public void incrementDeliveriesSent(Channel channel) {
    deliveriesSent
        .computeIfAbsent(channel, c -> registry.counter("deliveries_sent_total", "channel", tag(c)))
        .increment();
  }

  public void incrementDeliveriesFailed(Channel channel) {
    deliveriesFailed
        .computeIfAbsent(
            channel, c -> registry.counter("deliveries_failed_total", "channel", tag(c)))
        .increment();
  }

  public void incrementWebhookSent() {
    webhooksSent.increment();
  }

  public void incrementWebhookFailed() {
    webhooksFailed.increment();
  }

  public void recordSendLatency(Instant createdAt, Instant now) {
    if (createdAt != null && now != null) {
      sendLatency.record(Duration.between(createdAt, now));
    }
  }

  public void recordFailureLatency(Instant createdAt, Instant now) {
    if (createdAt != null && now != null) {
      sendFailureLatency.record(Duration.between(createdAt, now));
    }
  }
}
