package com.example.notifi.api.data.repository;

import com.example.notifi.api.data.entity.NotificationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository
    extends JpaRepository<NotificationEntity, UUID>, JpaSpecificationExecutor<NotificationEntity> {
  Optional<NotificationEntity> findByClientIdAndExternalRequestId(
      UUID clientId, String externalRequestId);

  Optional<NotificationEntity> findByIdAndClientId(UUID id, UUID clientId);
}
