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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification", schema = "public")
public class NotificationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "\"clientId\"", nullable = false)
    private UUID clientId;

    @Column(name = "\"externalRequestId\"", nullable = false)
    private String externalRequestId;

    @Column(name = "\"sendAt\"")
    private Instant sendAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    // "to" — зарезервированное слово в SQL, поэтому в схеме поле в кавычках
    @Column(name = "\"to\"", nullable = false)
    private String toAddress;

    @Column(name = "\"subject\"")
    private String subject;

    @Column(name = "\"templateCode\"")
    private String templateCode;

    @Convert(converter = JsonAttributesConverter.class)
    @Column(name = "variables", columnDefinition = "jsonb")
    private Map<String, Object> variables;

    @Column(name = "\"traceId\"")
    private String traceId;

    @Column(name = "\"webhookUrl\"")
    private String webhookUrl;

    @Column(name = "\"webhookSecret\"")
    private String webhookSecret;

    @Column(name = "\"attempts\"", nullable = false)
    private int attempts;

    @Column(name = "\"createdAt\"", nullable = false)
    private Instant createdAt;

    // Разрешаем NULL на первичном сохранении, если не выставили вручную
    @Column(name = "\"updatedAt\"")
    private Instant updatedAt;

    // --- JPA callbacks ---

    @PrePersist
    void prePersist() {
        final Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        // attempts может оставаться 0 на момент создания
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    // --- Business helpers ---

    public NotificationEntity markQueued(Instant now, int attempt) {
        this.status = NotificationStatus.QUEUED;
        this.updatedAt = now;
        this.attempts = attempt;
        return this;
    }

    public NotificationEntity markSent(Instant now, int attempt) {
        this.status = NotificationStatus.SENT;
        this.updatedAt = now;
        this.attempts = attempt;
        return this;
    }

    public NotificationEntity markFailed(Instant now, int attempt) {
        this.status = NotificationStatus.FAILED;
        this.updatedAt = now;
        this.attempts = attempt;
        return this;
    }

    // --- Getters / Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getExternalRequestId() {
        return externalRequestId;
    }

    public void setExternalRequestId(String externalRequestId) {
        this.externalRequestId = externalRequestId;
    }

    public Instant getSendAt() {
        return sendAt;
    }

    public void setSendAt(Instant sendAt) {
        this.sendAt = sendAt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
