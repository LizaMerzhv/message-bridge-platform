package com.example.notifi.api.core.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notifi.api.data.repository.NotificationDeliveryAttemptEntity;
import com.example.notifi.common.model.DeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationMapperTest {

  private final NotificationMapper mapper = new NotificationMapper();

  @Test
  void toView_ShouldIncludeDeliveryAttempts() {
    NotificationEntity entity = entity();
    NotificationDeliveryAttemptEntity attempt = attempt(1, DeliveryStatus.SENT, null, null);

    NotificationView view = mapper.toView(entity, List.of(attempt));

    assertThat(view.getId()).isEqualTo(entity.getId());
    assertThat(view.getStatus()).isEqualTo(entity.getStatus());
    assertThat(view.getDeliveries()).hasSize(1);
    DeliveryView delivery = view.getDeliveries().getFirst();
    assertThat(delivery.getAttempt()).isEqualTo(1);
    assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
    assertThat(delivery.getOccurredAt()).isEqualTo(attempt.getOccurredAt());
    assertThat(delivery.getCreatedAt()).isEqualTo(attempt.getOccurredAt());
    assertThat(delivery.getLastAttemptAt()).isEqualTo(attempt.getOccurredAt());
  }

  @Test
  void toDeliveryViews_ShouldHandleFailedAttemptsWithErrors() {
    NotificationDeliveryAttemptEntity first =
        attempt(1, DeliveryStatus.FAILED, "SMTP", "Mailbox full");
    NotificationDeliveryAttemptEntity second = attempt(2, DeliveryStatus.SENT, null, null);

    List<DeliveryView> deliveries = mapper.toDeliveryViews(List.of(first, second));

    assertThat(deliveries).hasSize(2);
    assertThat(deliveries.get(0).getErrorCode()).isEqualTo("SMTP");
    assertThat(deliveries.get(0).getErrorMessage()).isEqualTo("Mailbox full");
    assertThat(deliveries.get(1).getStatus()).isEqualTo(DeliveryStatus.SENT);
  }

  private NotificationEntity entity() {
    NotificationEntity entity = new NotificationEntity();
    entity.setId(UUID.randomUUID());
    entity.setClientId(UUID.randomUUID());
    entity.setChannel("EMAIL");
    entity.setTo("user@example.com");
    entity.setSubject("Subject");
    entity.setTemplateCode("WELCOME");
    entity.setExternalRequestId("ext-1");
    entity.setSendAt(Instant.parse("2024-01-01T00:00:00Z"));
    entity.setStatus(com.example.notifi.api.data.entity.NotificationStatus.SENT);
    entity.setAttempts(1);
    entity.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    entity.setUpdatedAt(Instant.parse("2024-01-01T00:00:05Z"));
    return entity;
  }

  private NotificationDeliveryAttemptEntity attempt(
      int number, DeliveryStatus status, String errorCode, String errorMessage) {
    NotificationDeliveryAttemptEntity entity = new NotificationDeliveryAttemptEntity();
    entity.setId(UUID.randomUUID());
    entity.setNotificationId(UUID.randomUUID());
    entity.setAttempt(number);
    entity.setStatus(status);
    entity.setErrorCode(errorCode);
    entity.setErrorMessage(errorMessage);
    entity.setOccurredAt(Instant.parse("2024-01-01T00:00:0" + number + "Z"));
    return entity;
  }
}
