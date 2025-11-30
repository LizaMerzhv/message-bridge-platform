package com.example.notifi.api.web.publicapi.notification;

import com.example.notifi.api.core.notification.CreateNotificationResult;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.security.SecurityUtils;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationRequest;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationPublicController {

  private final NotificationService notificationService;

  public NotificationPublicController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping
  public ResponseEntity<CreateNotificationResponse> create(
      @Valid @RequestBody CreateNotificationRequest request) {
    ClientPrincipal principal = requirePrincipal();
    CreateNotificationResult result = notificationService.create(request, principal);

    CreateNotificationResponse response =
        new CreateNotificationResponse()
            .setId(result.getEntity().getId())
            .setStatus(result.getEntity().getStatus().name())
            .setSendAtEffective(result.getEntity().getSendAt());

    if (result.isReplayed()) {
      return ResponseEntity.ok().header("X-Idempotency-Replayed", "true").body(response);
    }

    URI location =
        URI.create(String.format("/api/v1/notifications/%s", result.getEntity().getId()));
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{id}")
  public NotificationView getById(@PathVariable UUID id) {
    ClientPrincipal principal = requirePrincipal();
    return notificationService.findByIdForClient(id, principal.clientId());
  }

  private ClientPrincipal requirePrincipal() {
    ClientPrincipal principal = SecurityUtils.currentPrincipal();
    if (principal == null) {
      throw new IllegalStateException("Missing authenticated client");
    }
    return principal;
  }
}
