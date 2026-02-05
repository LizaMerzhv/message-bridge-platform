package com.example.notifi.api.web.internal.notification;

import com.example.notifi.api.core.notification.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/notifications")
@Tag(name = "Internal Notifications", description = "Internal callbacks for delivery results")
@Hidden
public class NotificationInternalController {

  private final NotificationService notificationService;

  public NotificationInternalController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping("/{id}/deliveries")
  @Operation(summary = "Record delivery result")
  public ResponseEntity<Void> updateStatus(
      @Parameter(description = "Notification identifier") @PathVariable UUID id,
      @Valid @RequestBody NotificationDeliveryUpdateRequest request) {

    notificationService.recordDeliveryResult(
        id,
        request.getStatus(),
        request.getAttempt(),
        request.getOccurredAt(),
        request.getErrorCode(),
        request.getErrorMessage());

    return ResponseEntity.noContent().build();
  }
}
