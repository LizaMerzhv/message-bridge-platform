package com.example.notifi.worker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.example.notifi.worker.amqp.RetryPolicy;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    private final RetryPolicy policy = new RetryPolicy();

    @Test
    void baseDelaysMatchSpecification() {
        assertThat(policy.baseDelayForAttempt(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.baseDelayForAttempt(2)).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.baseDelayForAttempt(3)).isEqualTo(Duration.ofSeconds(90));
    }

    @RepeatedTest(10)
    void jitterStaysWithinTwentyPercent() {
        Duration base = Duration.ofSeconds(30);
        Duration jittered = policy.jitteredDelay(base);
        assertThat(jittered)
                .isGreaterThanOrEqualTo(Duration.ofMillis(24_000))
                .isLessThanOrEqualTo(Duration.ofMillis(36_000));
    }

    @Test
    void minimumDelayMatchesSpecification() {
        Duration base = Duration.ofSeconds(90);
        assertThat(policy.minimumDelay(base)).isEqualTo(Duration.ofMillis(72_000));
        assertThat(policy.maximumDelay(base)).isEqualTo(Duration.ofMillis(108_000));
    }
}
