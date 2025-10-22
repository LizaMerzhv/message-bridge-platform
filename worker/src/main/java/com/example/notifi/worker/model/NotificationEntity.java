package com.example.notifi.worker.model;

import com.example.notifi.common.model.Channel;
import com.example.notifi.common.model.NotificationStatus;
import com.example.notifi.worker.util.JsonAttributesConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID clientId;

    @Column(nullable = false)
    private String externalRequestId;

    @Column(name = "\"send_at\"", nullable = false)
    private Instant sendAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(name = "\"to\"", nullable = false)
    private String toAddress;

    private String subject;

    private String templateCode;

    @Convert(converter = JsonAttributesConverter.class)
    private Map<String, Object> variables;

    private String traceId;

    private String webhookUrl;
    private String webhookSecret;

    @Column(name = "\"attempts\"", nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getExternalRequestId() { return externalRequestId; }
    public void setExternalRequestId(String externalRequestId) { this.externalRequestId = externalRequestId; }

    public Instant getSendAt() { return sendAt; }
    public void setSendAt(Instant sendAt) { this.sendAt = sendAt; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // --- domain helpers ---

    public void markQueued(Instant now) {
        this.status = NotificationStatus.QUEUED;
        this.updatedAt = now;
    }

    public void markSent(Instant now) {
        this.status = NotificationStatus.SENT;
        this.updatedAt = now;
    }

    public void markFailed(Instant now) {
        this.status = NotificationStatus.FAILED;
        this.updatedAt = now;
    }

    private void validateContent() {
        boolean subjectPresent = subject != null && !subject.isBlank();
        boolean templatePresent = templateCode != null && !templateCode.isBlank();
        boolean variablesPresent = variables != null && !variables.isEmpty();
        if (subjectPresent == (templatePresent && variablesPresent)) {
            throw new IllegalStateException("Notification content must satisfy XOR invariant");
        }
    }

    public NotificationSnapshot toSnapshot() {
        return new NotificationSnapshot(
            id, clientId, externalRequestId, channel, toAddress, subject, templateCode, variables, traceId);
    }

    public record NotificationSnapshot(
        UUID id,
        UUID clientId,
        String externalRequestId,
        Channel channel,
        String to,
        String subject,
        String templateCode,
        Map<String, Object> variables,
        String traceId) {}
}
