package com.example.notifi.worker.model;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageMapper {
  public NotificationMessage toMessage(NotificationEntity entity, int attempt) {
    Map<String, Object> variables = entity.getVariables();
    if (variables != null && variables.isEmpty()) {
      variables = null;
    }
    return new NotificationMessage(
        "1.0",
        entity.getId(),
        entity.getClientId(),
        entity.getExternalRequestId(),
        entity.getChannel(),
        entity.getToAddress(),
        entity.getSubject(),
        entity.getTemplateCode(),
        variables,
        attempt,
        entity.getTraceId());
  }
}
