package com.example.notifi.api.web.notification.dto;

import com.example.notifi.common.model.Channel;
import com.example.notifi.api.data.entity.NotificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NotificationView {
    private UUID id;
    private UUID clientId;
    private String externalRequestId;

    private Channel channel;
    private String to;
    private String subject;
    private String templateCode;

    private Map<String, Object> variables;

    private Instant sendAt;
    private NotificationStatus status;
    private int attempts;
    private Instant createdAt;
    private Instant updatedAt;

    private List<DeliveryView> deliveries;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getExternalRequestId() { return externalRequestId; }
    public void setExternalRequestId(String externalRequestId) { this.externalRequestId = externalRequestId; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }

    public Instant getSendAt() { return sendAt; }
    public void setSendAt(Instant sendAt) { this.sendAt = sendAt; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<DeliveryView> getDeliveries() { return deliveries; }
    public void setDeliveries(List<DeliveryView> deliveries) { this.deliveries = deliveries; }
}
