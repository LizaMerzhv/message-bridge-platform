package com.example.notifi.api.data.spec;

import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecifications {

    private NotificationSpecifications() {}

    public static Specification<NotificationEntity> belongsToClient(UUID clientId) {
        return (root, query, cb) -> cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<NotificationEntity> hasClient(UUID clientId) {
        return clientId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<NotificationEntity> hasStatus(NotificationStatus status) {
        return status == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<NotificationEntity> createdAtFrom(Instant from) {
        return from == null
                ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<NotificationEntity> createdAtTo(Instant to) {
        return to == null
                ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
