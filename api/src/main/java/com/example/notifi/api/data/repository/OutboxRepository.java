package com.example.notifi.api.data.repository;

import com.example.notifi.api.data.entity.OutboxEntity;
import com.example.notifi.api.data.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

  Optional<OutboxEntity> findByMessageKey(String messageKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from OutboxEntity o where o.status in :statuses order by o.createdAt")
  List<OutboxEntity> findNextBatchForUpdate(Collection<OutboxStatus> statuses, Pageable pageable);
}
