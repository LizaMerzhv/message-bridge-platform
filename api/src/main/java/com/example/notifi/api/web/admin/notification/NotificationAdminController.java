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
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
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
@RequestMapping("/admin/notifications")
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
    public PageResponse<NotificationSummaryDto> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "clientId", required = false) String clientId,
            @RequestParam(name = "createdFrom", required = false) String createdFrom,
            @RequestParam(name = "createdTo", required = false) String createdTo,
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
        Page<NotificationSummaryDto> page = notificationService.findAll(filter, pageable).map(mapper::toSummary);
        return PageResponse.from(page);
    }

    @GetMapping("/{id}")
    public NotificationDetailDto getById(@PathVariable UUID id) {
        NotificationView view = notificationService.findById(id);
        return mapper.toDetail(view);
    }

    @GetMapping("/{id}/attempts")
    public List<DeliveryAttemptDto> attempts(@PathVariable UUID id) {
        return mapper.toDeliveryAttempts(notificationService.findDeliveries(id));
    }

    @PostMapping
    public ResponseEntity<NotificationDetailDto> create(
            @Valid @RequestBody AdminCreateNotificationRequest request) {
        ClientPrincipal principal = resolvePrincipal(request.getClientId());
        CreateNotificationResult result = notificationService.create(request, principal);
        NotificationDetailDto body = mapper.toDetail(notificationService.findById(result.getEntity().getId()));
        URI location = URI.create(String.format("/admin/notifications/%s", body.getId()));
        return ResponseEntity.created(location).body(body);
    }

    private ClientPrincipal resolvePrincipal(UUID clientId) {
        return clientRepository
                .findById(clientId)
                .map(entity -> new ClientPrincipal(entity.getId(), entity.getName(), entity.getRateLimitPerMin()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown client: " + clientId));
    }
}
