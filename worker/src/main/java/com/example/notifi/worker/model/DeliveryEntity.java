package com.example.notifi.worker.model;

import com.example.notifi.common.model.Channel;
import com.example.notifi.common.model.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "delivery")
public class DeliveryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"notificationId\"", nullable = false)
    private NotificationEntity notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false)
    private int attempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(name = "\"to\"", nullable = false)
    private String recipient;

    private String subject;

    @Transient private String templateCode;

    @Transient private Map<String, Object> variables;

    @Column(name = "\"errorMessage\"")
    private String errorMessage;

    @Column(name = "\"errorCode\"")
    private String errorCode;

    @Column(name = "\"lastAttemptAt\"")
    private Instant lastAttemptAt;

    @Column(name = "\"createdAt\"", nullable = false)
    private Instant createdAt;

    public static DeliveryEntity create(NotificationEntity notification, int attempt) {
        DeliveryEntity entity = new DeliveryEntity();
        entity.notification = notification;
        entity.status = DeliveryStatus.PENDING;
        entity.attempt = attempt;
        entity.channel = notification.getChannel();
        entity.recipient = notification.getToAddress();
        entity.subject = notification.getSubject();
        entity.templateCode = notification.getTemplateCode();
        entity.variables = notification.getVariables();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public NotificationEntity getNotification() {
        return notification;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void markSent() {
        this.status = DeliveryStatus.SENT;
        this.errorMessage = null;
        this.errorCode = null;
    }

    public void markFailed(String message) {
        markFailed(message, null);
    }

    public void markFailed(String message, String code) {
        this.status = DeliveryStatus.FAILED;
        this.errorMessage = message;
        this.errorCode = code;
    }

    public int getAttempt() {
        return attempt;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public String getErrorMessage() {
        return errorMessage;
    }


    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
    }
}
