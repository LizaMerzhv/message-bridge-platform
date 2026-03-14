package com.example.notifi.worker.data.repository;

import com.example.notifi.worker.data.entity.DeliveryEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {
  Optional<DeliveryEntity> findFirstByNotificationIdOrderByAttemptDesc(UUID notificationId);
}
