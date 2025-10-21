package com.example.notifi.worker.amqp;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

public class RetryPolicy {

    private static final Map<Integer, Duration> ATTEMPT_BASE =
        Map.of(
            1, Duration.ofSeconds(10),
            2, Duration.ofSeconds(30),
            3, Duration.ofSeconds(90));

    private final DoubleSupplier randomSupplier;
    private final int maxAttempts;

    public RetryPolicy(DoubleSupplier randomSupplier, int maxAttempts) {
        this.randomSupplier = Objects.requireNonNull(randomSupplier, "randomSupplier");
        this.maxAttempts = maxAttempts;
    }

    public RetryPolicy() {
        this(ThreadLocalRandom.current()::nextDouble, 3);
    }

    public Duration baseDelayForAttempt(int attempt) {
        return ATTEMPT_BASE.getOrDefault(attempt, Duration.ofSeconds(90));
    }

    public Duration minimumDelay(Duration base) {
        return scale(base, 0.8);
    }

    public Duration maximumDelay(Duration base) {
        return scale(base, 1.2);
    }

    public Duration jitteredDelay(Duration base) {
        long min = minimumDelay(base).toMillis();
        long max = maximumDelay(base).toMillis();
        long span = Math.max(0, max - min);
        long val = min + Math.round(span * randomSupplier.getAsDouble());
        return Duration.ofMillis(val);
    }

    public RetryDecision evaluate(int currentAttempt) {
        if (currentAttempt >= maxAttempts) {
            return RetryDecision.noRetry();
        }
        int nextAttempt = currentAttempt + 1;

        Duration base = baseDelayForAttempt(currentAttempt);
        Duration ttl  = minimumDelay(base);
        Duration jitter = Duration.ZERO;

        return RetryDecision.retry(nextAttempt, ttl, jitter);
    }

    private static Duration scale(Duration base, double factor) {
        return Duration.ofMillis(Math.round(base.toMillis() * factor));
    }

    public record RetryDecision(boolean shouldRetry, int nextAttempt, Duration ttl, Duration additionalDelay) {
        public static RetryDecision noRetry() {
            return new RetryDecision(false, 0, Duration.ZERO, Duration.ZERO);
        }
        public static RetryDecision retry(int nextAttempt, Duration ttl, Duration additionalDelay) {
            return new RetryDecision(true, nextAttempt, ttl, additionalDelay);
        }
    }
}
