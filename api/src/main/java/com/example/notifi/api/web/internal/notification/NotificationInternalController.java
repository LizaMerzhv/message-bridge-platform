package com.example.notifi.api.web.internal.notification;

import com.example.notifi.api.core.notification.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class NotificationInternalController {

  private final NotificationService notificationService;

  public NotificationInternalController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping("/{id}/deliveries")
  public ResponseEntity<Void> updateStatus(
      @PathVariable UUID id, @Valid @RequestBody NotificationDeliveryUpdateRequest request) {
    notificationService.recordDeliveryResult(
        id, request.getStatus(), request.getAttemptedAt(), request.getErrorMessage());
    return ResponseEntity.noContent().build();
  }
}
