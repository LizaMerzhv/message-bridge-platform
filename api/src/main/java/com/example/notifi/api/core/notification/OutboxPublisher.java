package com.example.notifi.api.core.notification;

import com.example.notifi.api.data.entity.OutboxEntity;
import com.example.notifi.api.data.entity.OutboxStatus;
import com.example.notifi.api.data.repository.OutboxRepository;
import com.example.notifi.common.messaging.AmqpConstants;
import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final String exchange;
    private final String routingKey;
    private final int batchSize;

    public OutboxPublisher(
        OutboxRepository outboxRepository,
        ObjectMapper objectMapper,
        RabbitTemplate rabbitTemplate,
        Clock clock,
        @Value("${notifi.amqp.exchange:" + AmqpConstants.DEFAULT_EXCHANGE + "}") String exchange,
        @Value("${notifi.amqp.ingest-routing-key:" + AmqpConstants.INGEST_ROUTING_KEY + "}") String routingKey,
        @Value("${notifi.outbox.batch-size:25}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${notifi.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEntity> batch =
            outboxRepository.findNextBatchForUpdate(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED), PageRequest.of(0, batchSize));

        if (batch.isEmpty()) {
            return;
        }

        log.info("Publishing {} outbox messages", batch.size());
        Instant now = clock.instant();

        for (OutboxEntity outbox : batch) {
            try {
                NotificationTaskMessage message =
                    objectMapper.readValue(outbox.getPayload(), NotificationTaskMessage.class);
                rabbitTemplate.convertAndSend(exchange, routingKey, message, messageId(outbox));

                outbox.setStatus(OutboxStatus.PUBLISHED);
                outbox.setPublishedAt(now);
                outbox.setLastAttemptAt(now);
            } catch (Exception ex) {
                outbox.setAttempts(outbox.getAttempts() + 1);
                outbox.setLastAttemptAt(now);
                outbox.setStatus(OutboxStatus.FAILED);
                log.error("Failed to publish outbox message {} with key {}", outbox.getId(), outbox.getMessageKey(), ex);
            }

            outbox.setUpdatedAt(now);
        }

        outboxRepository.saveAll(batch);
    }

    private MessagePostProcessor messageId(OutboxEntity outbox) {
        return message -> {
            message.getMessageProperties().setMessageId(outbox.getId().toString());
            return message;
        };
    }
}
