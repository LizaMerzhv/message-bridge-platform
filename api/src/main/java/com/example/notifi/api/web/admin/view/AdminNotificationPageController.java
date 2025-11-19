package com.example.notifi.api.web.admin.view;

import com.example.notifi.api.core.notification.NotificationFilter;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.web.admin.notification.NotificationAdminMapper;
import com.example.notifi.api.web.admin.notification.dto.NotificationSummaryDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/ui/notifications")
public class AdminNotificationPageController {

    private final NotificationService notificationService;
    private final NotificationAdminMapper mapper;

    public AdminNotificationPageController(
        NotificationService notificationService, NotificationAdminMapper mapper) {
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @GetMapping
    public String list(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "clientId", required = false) String clientId,
        @PageableDefault(size = 20) Pageable pageable,
        Model model) {
        NotificationFilter filter = new NotificationFilter();
        if (StringUtils.hasText(status)) {
            try {
                filter.setStatus(NotificationStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // keep filter empty when status is invalid
            }
        }
        if (StringUtils.hasText(clientId)) {
            try {
                filter.setClientId(UUID.fromString(clientId));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid UUID input
            }
        }

        Page<NotificationSummaryDto> page =
            notificationService.findAll(filter, pageable).map(mapper::toSummary);

        model.addAttribute("page", page);
        model.addAttribute("statusFilter", status);
        model.addAttribute("clientFilter", clientId);
        model.addAttribute("availableStatuses", NotificationStatus.values());
        return "admin/notifications";
    }
}
