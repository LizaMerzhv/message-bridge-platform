package com.example.notifi.api.messaging;

import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.security.ClientPrincipal;

public interface NotificationPublisher {
    void publish(NotificationEntity notification, ClientPrincipal principal, String traceId);
}
