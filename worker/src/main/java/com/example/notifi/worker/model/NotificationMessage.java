package com.example.notifi.worker.model;

import com.example.notifi.common.model.Channel;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationMessage(
    String schemaVersion,
    UUID notificationId,
    UUID clientId,
    String externalRequestId,
    Channel channel,
    String to,
    String subject,
    String templateCode,
    Map<String, Object> variables,
    int attempt,
    String traceId) {

  public NotificationMessage withAttempt(int nextAttempt) {
    return new NotificationMessage(
        schemaVersion,
        notificationId,
        clientId,
        externalRequestId,
        channel,
        to,
        subject,
        templateCode,
        variables,
        nextAttempt,
        traceId);
  }
}
