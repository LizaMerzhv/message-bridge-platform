package com.example.notifi.api.web.admin.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notifi.api.core.notification.DeliveryView;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.common.model.DeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationAdminMapperTest {

  private final NotificationAdminMapper mapper = new NotificationAdminMapper();

  @Test
  void toSummary_ShouldMapTopLevelFields() {
    NotificationView view = view();

    var dto = mapper.toSummary(view);

    assertThat(dto.getId()).isEqualTo(view.getId());
    assertThat(dto.getClientId()).isEqualTo(view.getClientId());
    assertThat(dto.getStatus()).isEqualTo("CREATED");
    assertThat(dto.getAttempts()).isEqualTo(1);
  }

  @Test
  void toDetail_ShouldResolveDeliveryTimestampFallbacks() {
    DeliveryView missingOccurred = new DeliveryView();
    missingOccurred.setAttempt(1);
    missingOccurred.setStatus(DeliveryStatus.FAILED);
    missingOccurred.setCreatedAt(Instant.parse("2024-01-01T00:00:02Z"));
    missingOccurred.setLastAttemptAt(Instant.parse("2024-01-01T00:00:03Z"));

    DeliveryView withOccurred = new DeliveryView();
    withOccurred.setAttempt(2);
    withOccurred.setStatus(DeliveryStatus.SENT);
    withOccurred.setOccurredAt(Instant.parse("2024-01-01T00:00:05Z"));
    withOccurred.setCreatedAt(Instant.parse("2024-01-01T00:00:05Z"));

    NotificationView view = view();
    view.setDeliveries(List.of(missingOccurred, withOccurred));

    var detail = mapper.toDetail(view);

    assertThat(detail.getDeliveries()).hasSize(2);
    assertThat(detail.getDeliveries().get(0).getTimestamp())
        .isEqualTo(Instant.parse("2024-01-01T00:00:03Z"));
    assertThat(detail.getDeliveries().get(1).getTimestamp())
        .isEqualTo(Instant.parse("2024-01-01T00:00:05Z"));
  }

  private NotificationView view() {
    NotificationView view = new NotificationView();
    view.setId(UUID.randomUUID());
    view.setClientId(UUID.randomUUID());
    view.setChannel("EMAIL");
    view.setTo("user@example.com");
    view.setSubject("Subject");
    view.setTemplateCode("WELCOME");
    view.setStatus(NotificationStatus.CREATED);
    view.setAttempts(1);
    view.setSendAt(Instant.parse("2024-01-01T00:00:00Z"));
    view.setSendAtEffective(view.getSendAt());
    view.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    view.setUpdatedAt(Instant.parse("2024-01-01T00:00:01Z"));
    return view;
  }
}
