package com.example.notifi.api.data.repository;

import com.example.notifi.api.data.entity.DeliveryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {
  List<DeliveryEntity> findByNotificationIdOrderByAttemptAsc(UUID notificationId);
}
