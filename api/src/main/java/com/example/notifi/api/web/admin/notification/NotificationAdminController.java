package com.example.notifi.api.web.admin.notification;

import com.example.notifi.api.core.notification.CreateNotificationResult;
import com.example.notifi.api.core.notification.NotificationFilter;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.admin.dto.PageResponse;
import com.example.notifi.api.web.admin.notification.dto.AdminCreateNotificationRequest;
import com.example.notifi.api.web.admin.notification.dto.DeliveryAttemptDto;
import com.example.notifi.api.web.admin.notification.dto.NotificationDetailDto;
import com.example.notifi.api.web.admin.notification.dto.NotificationSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notifications")
@Tag(name = "Admin Notifications", description = "Administrative operations for notifications")
public class NotificationAdminController {

  private final NotificationService notificationService;
  private final ClientRepository clientRepository;
  private final NotificationAdminMapper mapper;

  public NotificationAdminController(
      NotificationService notificationService,
      ClientRepository clientRepository,
      NotificationAdminMapper mapper) {
    this.notificationService = notificationService;
    this.clientRepository = clientRepository;
    this.mapper = mapper;
  }

  @GetMapping
  @Operation(
      summary = "List notifications",
      description = "Returns paged notifications with optional filters")
  public PageResponse<NotificationSummaryDto> list(
      @Parameter(description = "Filter by status", example = "SENT")
          @RequestParam(name = "status", required = false)
          String status,
      @Parameter(description = "Filter by client id")
          @RequestParam(name = "clientId", required = false)
          String clientId,
      @Parameter(
              description = "Created from timestamp in ISO-8601",
              example = "2024-06-10T12:00:00Z")
          @RequestParam(name = "createdFrom", required = false)
          String createdFrom,
      @Parameter(description = "Created to timestamp in ISO-8601", example = "2024-06-18T12:00:00Z")
          @RequestParam(name = "createdTo", required = false)
          String createdTo,
      Pageable pageable) {
    NotificationFilter filter = new NotificationFilter();
    if (StringUtils.hasText(status)) {
      filter.setStatus(NotificationStatus.valueOf(status.toUpperCase()));
    }
    if (StringUtils.hasText(clientId)) {
      filter.setClientId(UUID.fromString(clientId));
    }
    if (StringUtils.hasText(createdFrom)) {
      filter.setCreatedFrom(Instant.parse(createdFrom));
    }
    if (StringUtils.hasText(createdTo)) {
      filter.setCreatedTo(Instant.parse(createdTo));
    }
    Page<NotificationSummaryDto> page =
        notificationService.findAll(filter, pageable).map(mapper::toSummary);
    return PageResponse.from(page);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get notification by id")
  public NotificationDetailDto getById(
      @Parameter(description = "Notification identifier") @PathVariable UUID id) {
    NotificationView view = notificationService.findById(id);
    return mapper.toDetail(view);
  }

  @GetMapping("/{id}/attempts")
  @Operation(summary = "List delivery attempts")
  public List<DeliveryAttemptDto> attempts(
      @Parameter(description = "Notification identifier") @PathVariable UUID id) {
    return mapper.toDeliveryAttempts(notificationService.findDeliveries(id));
  }

  @PostMapping
  @Operation(summary = "Create notification on behalf of a client")
  public ResponseEntity<NotificationDetailDto> create(
      @Valid @RequestBody AdminCreateNotificationRequest request) {
    ClientPrincipal principal = resolvePrincipal(request.getClientId());
    CreateNotificationResult result = notificationService.create(request, principal);
    NotificationDetailDto body =
        mapper.toDetail(notificationService.findById(result.getEntity().getId()));
    URI location = URI.create(String.format("/admin/notifications/%s", body.getId()));
    return ResponseEntity.created(location).body(body);
  }

  private ClientPrincipal resolvePrincipal(UUID clientId) {
    return clientRepository
        .findById(clientId)
        .map(
            entity ->
                new ClientPrincipal(entity.getId(), entity.getName(), entity.getRateLimitPerMin()))
        .orElseThrow(() -> new IllegalArgumentException("Unknown client: " + clientId));
  }
}
