package com.example.notifi.api.web.notification;

import com.example.notifi.api.core.notification.CreateNotificationResult;
import com.example.notifi.api.core.notification.NotificationFilter;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.security.SecurityUtils;
import com.example.notifi.common.dto.CreateNotificationRequest;
import com.example.notifi.common.dto.CreateNotificationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<CreateNotificationResponse> create(
        @Valid @RequestBody CreateNotificationRequest request) {
        ClientPrincipal principal = requirePrincipal();
        CreateNotificationResult result = notificationService.create(request, principal);
        NotificationEntity entity = result.getEntity();

        Instant sendAtEffective = entity.getSendAt();

        CreateNotificationResponse response = new CreateNotificationResponse()
            .setId(entity.getId())
            .setStatus(entity.getStatus().name())
            .setSendAtEffective(sendAtEffective);

        if (result.isReplayed()) {
            return ResponseEntity.ok()
                .header("X-Idempotency-Replayed", "true")
                .body(response);
        }

        URI location = URI.create(String.format("/api/v1/notifications/%s", entity.getId()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public NotificationView getById(@PathVariable UUID id) {
        ClientPrincipal principal = requirePrincipal();
        return notificationService.findByIdForClient(id, principal.clientId());
    }

    @GetMapping
    public Page<NotificationView> list(
            HttpServletRequest request,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "createdFrom", required = false) String createdFrom,
            @RequestParam(name = "createdTo", required = false) String createdTo,
            Pageable pageable) {
        ClientPrincipal principal = requirePrincipal();
        if (StringUtils.hasText(request.getParameter("clientId"))) {
            throw new IllegalArgumentException("clientId parameter is not supported");
        }
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Page size must be <= 100");
        }
        NotificationFilter filter = new NotificationFilter();
        if (StringUtils.hasText(status)) {
            filter.setStatus(NotificationStatus.valueOf(status.toUpperCase()));
        }
        if (StringUtils.hasText(createdFrom)) {
            filter.setCreatedFrom(Instant.parse(createdFrom));
        }
        if (StringUtils.hasText(createdTo)) {
            filter.setCreatedTo(Instant.parse(createdTo));
        }
        return notificationService.findAllForClient(filter, principal.clientId(), pageable);
    }

    private ClientPrincipal requirePrincipal() {
        ClientPrincipal principal = SecurityUtils.currentPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Missing authenticated client");
        }
        return principal;
    }
}
