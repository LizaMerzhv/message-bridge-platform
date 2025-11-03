package com.example.notifi.worker.model;

import com.example.notifi.common.messaging.NotificationTaskMessage;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageMapper {
    public NotificationTaskMessage toMessage(NotificationEntity entity, int attempt) {
        Map<String, Object> variables = entity.getVariables();
        if (variables != null && variables.isEmpty()) {
            variables = null;
        }
            return new NotificationTaskMessage(
                entity.getId(), // keep worker payload aligned with API persistence id
                entity.getClientId(), // maintain client ownership for webhook payloads
                entity.getExternalRequestId(), // propagate idempotency key for logging
                entity.getChannel(), // enforce channel specific routing
                entity.getToAddress(), // destination email resolved by API
                entity.getSubject(), // subject stored in worker DB
                entity.getTemplateCode(), // template code snapshot for templating
                variables, // runtime variables captured from API
                entity.getSendAt(), // ensure scheduler respects original send time
                entity.getCreatedAt(), // preserve creation timestamp for metrics
                attempt, // inform consumer about attempt count
                entity.getTraceId(), // correlate events across services
                entity.getWebhookUrl(), // allow webhook dispatcher to target client
                entity.getWebhookSecret()); // keep webhook signing secret close to worker logic
        }
    }
