package com.example.notifi.worker.amqp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notifi.worker.amqp.RetryPolicy.RetryDecision;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;

class RetryPolicyEvaluateTest {

  @Test
  void computesMinDelayWithJitter() {
    AtomicReference<Double> value = new AtomicReference<>(0.0d);
    RetryPolicy policy = new RetryPolicy(new StubRandom(value), 3);

    value.set(0.0d);
    RetryDecision first = policy.evaluate(1);
    assertThat(first.shouldRetry()).isTrue();
    assertThat(first.ttl()).isEqualTo(Duration.ofSeconds(8));
    assertThat(first.additionalDelay()).isEqualTo(Duration.ZERO);

    value.set(1.0d);
    RetryDecision second = policy.evaluate(2);
    assertThat(second.ttl()).isEqualTo(Duration.ofSeconds(24));
    assertThat(second.additionalDelay().toMillis()).isBetween(0L, 12000L);

    RetryDecision finalAttempt = policy.evaluate(3);
    assertThat(finalAttempt.shouldRetry()).isFalse();
  }

  private static class StubRandom implements DoubleSupplier {
    private final AtomicReference<Double> value;

    private StubRandom(AtomicReference<Double> value) {
      this.value = value;
    }

    @Override
    public double getAsDouble() {
      return value.get();
    }
  }
}
