package com.example.notifi.api.web.admin.notification.dto;

import java.time.Instant;

public class DeliveryAttemptDto {
    private int attempt;
    private String status;
    private String channel;
    private String to;
    private String subject;
    private String errorCode;
    private String errorMessage;
    private Instant timestamp;

    public int getAttempt() {
        return attempt;
    }

    public DeliveryAttemptDto setAttempt(int attempt) {
        this.attempt = attempt;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public DeliveryAttemptDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getChannel() {
        return channel;
    }

    public DeliveryAttemptDto setChannel(String channel) {
        this.channel = channel;
        return this;
    }

    public String getTo() {
        return to;
    }

    public DeliveryAttemptDto setTo(String to) {
        this.to = to;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public DeliveryAttemptDto setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public DeliveryAttemptDto setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DeliveryAttemptDto setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public DeliveryAttemptDto setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
