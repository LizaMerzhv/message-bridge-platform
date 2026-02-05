package com.example.notifi.worker.data.repository;

import com.example.notifi.common.model.NotificationStatus;
import com.example.notifi.worker.data.entity.NotificationEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

  @Query(
      value =
          """
                SELECT * FROM notification
                WHERE status = :status AND send_at <= :cutoff
                ORDER BY send_at, id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """,
      nativeQuery = true)
  List<NotificationEntity> lockNextBatch(
      @Param("status") String status, @Param("cutoff") Instant cutoff, @Param("limit") int limit);

  default List<NotificationEntity> lockCreatedBefore(Instant cutoff, int limit) {
    return lockNextBatch(NotificationStatus.CREATED.name(), cutoff, limit);
  }
}
