package com.example.notifi.api.core.notification;

import com.example.notifi.api.data.entity.ClientEntity;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.common.messaging.AmqpConstants;
import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.example.notifi.common.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publishes notification creation events for the worker service.
 */
@Component
public class NotificationTaskPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationTaskPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public NotificationTaskPublisher(
        RabbitTemplate rabbitTemplate,
        @Value("${notifi.amqp.exchange:" + AmqpConstants.DEFAULT_EXCHANGE + "}") String exchange,
        @Value("${notifi.amqp.ingest-routing-key:" + AmqpConstants.INGEST_ROUTING_KEY + "}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(NotificationEntity entity, ClientEntity client) {
        Channel channel = Channel.from(entity.getChannel());
        NotificationTaskMessage payload = new NotificationTaskMessage(
            entity.getId(),
            entity.getClientId(),
            entity.getExternalRequestId(),
            channel,
            entity.getTo(),
            entity.getSubject(),
            entity.getTemplateCode(),
            entity.getVariables(),
            entity.getSendAt(),
            entity.getCreatedAt(),
            0,
            MDC.get("traceId"),
            client.getWebhookUrl(),
            client.getWebhookSecret());
        log.info(
            "Publishing notification {} with status {} scheduled at {}",
            entity.getId(),
            entity.getStatus(),
            entity.getSendAt());
        rabbitTemplate.convertAndSend(exchange, routingKey, payload); // emit event instead of sharing DB tables
    }
}
