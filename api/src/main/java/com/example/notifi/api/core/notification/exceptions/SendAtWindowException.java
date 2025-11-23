package com.example.notifi.api.core.notification.exceptions;

import java.time.Instant;

public class SendAtWindowException extends RuntimeException {

    private final Instant sendAt;
    private final Instant min;
    private final Instant max;

    public SendAtWindowException(Instant sendAt, Instant min, Instant max) {
        super("sendAt must be between " + min + " and " + max);
        this.sendAt = sendAt;
        this.min = min;
        this.max = max;
    }

    public Instant getSendAt() {
        return sendAt;
    }

    public Instant getMin() {
        return min;
    }

    public Instant getMax() {
        return max;
    }
}
