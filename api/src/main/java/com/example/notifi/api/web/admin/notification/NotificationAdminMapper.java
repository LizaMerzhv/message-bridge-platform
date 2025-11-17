package com.example.notifi.api.web.admin.notification;

import com.example.notifi.api.core.notification.DeliveryView;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.web.admin.notification.dto.DeliveryAttemptDto;
import com.example.notifi.api.web.admin.notification.dto.NotificationDetailDto;
import com.example.notifi.api.web.admin.notification.dto.NotificationSummaryDto;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationAdminMapper {

    public NotificationSummaryDto toSummary(NotificationView view) {
        return new NotificationSummaryDto()
                .setId(view.getId())
                .setClientId(view.getClientId())
                .setTo(view.getTo())
                .setStatus(view.getStatus().name())
                .setSendAt(view.getSendAt())
                .setCreatedAt(view.getCreatedAt())
                .setAttempts(view.getAttempts());
    }

    public NotificationDetailDto toDetail(NotificationView view) {
        NotificationDetailDto dto = new NotificationDetailDto()
                .setId(view.getId())
                .setClientId(view.getClientId())
                .setChannel(view.getChannel())
                .setTo(view.getTo())
                .setStatus(view.getStatus().name())
                .setSubject(view.getSubject())
                .setTemplateCode(view.getTemplateCode())
                .setVariables(view.getVariables())
                .setExternalRequestId(view.getExternalRequestId())
                .setSendAt(view.getSendAt())
                .setSendAtEffective(view.getSendAtEffective())
                .setCreatedAt(view.getCreatedAt())
                .setUpdatedAt(view.getUpdatedAt())
                .setAttempts(view.getAttempts());
        dto.setDeliveries(view.getDeliveries().stream().map(this::toDelivery).toList());
        return dto;
    }

    public List<DeliveryAttemptDto> toDeliveryAttempts(List<DeliveryView> deliveries) {
        return deliveries.stream().map(this::toDelivery).toList();
    }

    private DeliveryAttemptDto toDelivery(DeliveryView view) {
        Instant timestamp = view.getLastAttemptAt() != null ? view.getLastAttemptAt() : view.getCreatedAt();
        return new DeliveryAttemptDto()
                .setAttempt(view.getAttempt())
                .setStatus(view.getStatus().name())
                .setChannel(view.getChannel())
                .setTo(view.getTo())
                .setSubject(view.getSubject())
                .setErrorCode(view.getErrorCode())
                .setErrorMessage(view.getErrorMessage())
                .setTimestamp(timestamp);
    }
}
