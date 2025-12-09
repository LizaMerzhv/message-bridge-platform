package com.example.notifi.api.core.notification;

import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.repository.NotificationDeliveryAttemptEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

  public NotificationView toView(
      NotificationEntity entity, List<NotificationDeliveryAttemptEntity> deliveries) {
    NotificationView view = toView(entity);
    view.setDeliveries(toDeliveryViews(deliveries));
    return view;
  }

  public List<DeliveryView> toDeliveryViews(List<NotificationDeliveryAttemptEntity> deliveries) {
    return deliveries.stream().map(this::toDeliveryView).collect(Collectors.toList());
  }

  public NotificationView toView(NotificationEntity entity) {
    NotificationView view = new NotificationView();
    view.setId(entity.getId());
    view.setClientId(entity.getClientId());
    view.setChannel(entity.getChannel());
    view.setTo(entity.getTo());
    view.setSubject(entity.getSubject());
    view.setTemplateCode(entity.getTemplateCode());
    view.setVariables(entity.getVariables());
    view.setExternalRequestId(entity.getExternalRequestId());
    view.setSendAt(entity.getSendAt());
    view.setSendAtEffective(entity.getSendAt());
    view.setStatus(entity.getStatus());
    view.setAttempts(entity.getAttempts());
    view.setCreatedAt(entity.getCreatedAt());
    view.setUpdatedAt(entity.getUpdatedAt());
    view.setDeliveries(java.util.Collections.emptyList());
    return view;
  }

  private DeliveryView toDeliveryView(NotificationDeliveryAttemptEntity entity) {
    DeliveryView view = new DeliveryView();
    view.setAttempt(entity.getAttempt());
    view.setStatus(entity.getStatus());
    view.setErrorCode(entity.getErrorCode());
    view.setErrorMessage(entity.getErrorMessage());
    view.setOccurredAt(entity.getOccurredAt());
    view.setCreatedAt(entity.getOccurredAt());
    view.setLastAttemptAt(entity.getOccurredAt());
    return view;
  }
}
