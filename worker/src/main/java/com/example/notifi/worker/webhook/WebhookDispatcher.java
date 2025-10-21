package com.example.notifi.worker.webhook;

import com.example.notifi.worker.model.NotificationEntity;

public interface WebhookDispatcher {
  void dispatch(NotificationEntity notification);
}
