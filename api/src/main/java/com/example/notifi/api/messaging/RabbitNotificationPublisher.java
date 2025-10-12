package com.example.notifi.api.messaging;

import com.example.notifi.api.config.AmqpProperties;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.security.ClientPrincipal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitNotificationPublisher implements NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpProperties properties;
    private final DirectExchange exchange;

    public RabbitNotificationPublisher(
            RabbitTemplate rabbitTemplate, AmqpProperties properties, DirectExchange exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.exchange = exchange;
    }

    @Override
    public void publish(NotificationEntity notification, ClientPrincipal principal, String traceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("notificationId", notification.getId());
        payload.put("clientId", notification.getClientId());
        payload.put("externalRequestId", notification.getExternalRequestId());
        payload.put("channel", notification.getChannel());
        payload.put("to", notification.getTo());
        payload.put("attempt", notification.getAttempts());
        payload.put("traceId", traceId);
        if (notification.getSubject() != null) {
            payload.put("subject", notification.getSubject());
        }
        if (notification.getTemplateCode() != null) {
            payload.put("templateCode", notification.getTemplateCode());
            payload.put("variables", notification.getVariables());
        }
        rabbitTemplate.convertAndSend(exchange.getName(), properties.getRouting().getTasks(), payload);
    }
}
