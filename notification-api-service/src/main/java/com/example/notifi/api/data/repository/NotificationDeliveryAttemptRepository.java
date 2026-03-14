package com.example.notifi.api.data.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryAttemptRepository
    extends JpaRepository<NotificationDeliveryAttemptEntity, UUID> {

  List<NotificationDeliveryAttemptEntity> findByNotificationIdOrderByAttemptAsc(
      UUID notificationId);

  Optional<NotificationDeliveryAttemptEntity> findByNotificationIdAndAttempt(
      UUID notificationId, int attempt);
}
