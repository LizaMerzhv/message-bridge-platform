package com.example.notifi.worker.webhook;

import com.example.notifi.worker.data.entity.NotificationEntity;

public interface WebhookDispatcher {
  void dispatch(NotificationEntity notification);
}
