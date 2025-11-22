package com.example.notifi.api.web.admin.view;

import com.example.notifi.api.core.notification.CreateNotificationResult;
import com.example.notifi.api.core.notification.NotificationFilter;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.admin.notification.NotificationAdminMapper;
import com.example.notifi.api.web.admin.notification.dto.AdminCreateNotificationRequest;
import com.example.notifi.api.web.admin.notification.dto.DeliveryAttemptDto;
import com.example.notifi.api.web.admin.notification.dto.NotificationDetailDto;
import com.example.notifi.api.web.admin.notification.dto.NotificationSummaryDto;
import com.example.notifi.common.model.Channel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ui/notifications")
public class AdminNotificationPageController {

    private final NotificationService notificationService;
    private final NotificationAdminMapper mapper;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    public AdminNotificationPageController(
        NotificationService notificationService,
        NotificationAdminMapper mapper,
        ClientRepository clientRepository,
        ObjectMapper objectMapper
    ) {
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------
    // LIST PAGE
    // ---------------------------------------------------------

    @GetMapping
    public String list(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "clientId", required = false) String clientId,
        @PageableDefault(size = 20) Pageable pageable,
        Model model
    ) {
        NotificationFilter filter = buildFilter(status, clientId);

        Page<NotificationSummaryDto> page =
            notificationService.findAll(filter, pageable).map(mapper::toSummary);

        long sentCount = notificationService.countByStatus(filter, NotificationStatus.SENT);
        long failedCount = notificationService.countByStatus(filter, NotificationStatus.FAILED);

        model.addAttribute("page", page);
        model.addAttribute("statusFilter", status);
        model.addAttribute("clientFilter", clientId);
        model.addAttribute("availableStatuses", NotificationStatus.values());
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("activePage", "notifications");

        return "admin/notifications";
    }

    // ---------------------------------------------------------
    // CREATE FORM
    // ---------------------------------------------------------

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("notificationForm")) {
            NotificationForm form = new NotificationForm();
            form.setChannel(Channel.EMAIL.name());
            model.addAttribute("notificationForm", form);
        }
        model.addAttribute("activePage", "notifications");
        return "admin/notification-new";
    }

    // ---------------------------------------------------------
    // CREATE ACTION
    // ---------------------------------------------------------

    @PostMapping
    public String create(
        @Valid @ModelAttribute("notificationForm") NotificationForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {

        validateContentChoice(form, bindingResult);

        UUID clientId = parseClientId(form.getClientId(), bindingResult);
        Instant sendAt = parseSendAt(form.getSendAt(), bindingResult);
        Map<String, Object> variables = parseVariables(form.getVariables(), bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "notifications");
            return "admin/notification-new";
        }

        AdminCreateNotificationRequest request = new AdminCreateNotificationRequest();
        request.setClientId(clientId);
        request.setChannel(Channel.valueOf(form.getChannel().toUpperCase()));
        request.setTo(form.getTo());
        request.setSubject(StringUtils.hasText(form.getSubject()) ? form.getSubject() : null);
        request.setTemplateCode(StringUtils.hasText(form.getTemplateCode()) ? form.getTemplateCode() : null);
        request.setVariables(variables);
        request.setSendAt(sendAt);
        request.setExternalRequestId(form.getExternalRequestId());

        ClientPrincipal principal = clientRepository
            .findById(clientId)
            .map(entity -> new ClientPrincipal(entity.getId(), entity.getName(), entity.getRateLimitPerMin()))
            .orElseThrow(() -> new IllegalArgumentException("Unknown client: " + clientId));

        CreateNotificationResult result = notificationService.create(request, principal);

        redirectAttributes.addFlashAttribute("created", true);
        return "redirect:/admin/ui/notifications/" + result.getEntity().getId();
    }

    // ---------------------------------------------------------
    // DETAIL PAGE
    // ---------------------------------------------------------

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        NotificationDetailDto notification = mapper.toDetail(notificationService.findById(id));
        List<DeliveryAttemptDto> attempts = mapper.toDeliveryAttempts(notificationService.findDeliveries(id));

        model.addAttribute("notification", notification);
        model.addAttribute("attempts", attempts);
        model.addAttribute("activePage", "notifications");

        return "admin/notification-detail";
    }

    // ---------------------------------------------------------
    // UTIL METHODS
    // ---------------------------------------------------------

    private NotificationFilter buildFilter(String status, String clientId) {
        NotificationFilter filter = new NotificationFilter();

        if (StringUtils.hasText(status)) {
            try {
                filter.setStatus(NotificationStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (StringUtils.hasText(clientId)) {
            try {
                filter.setClientId(UUID.fromString(clientId));
            } catch (IllegalArgumentException ignored) {}
        }

        return filter;
    }

    private UUID parseClientId(String clientId, BindingResult bindingResult) {
        try {
            return UUID.fromString(clientId);
        } catch (Exception ex) {
            bindingResult.rejectValue("clientId", "invalid", "Client ID must be a valid UUID");
            return null;
        }
    }

    private Instant parseSendAt(String sendAt, BindingResult bindingResult) {
        if (!StringUtils.hasText(sendAt)) return null;

        try {
            return Instant.parse(sendAt);
        } catch (DateTimeParseException ex) {
            bindingResult.rejectValue("sendAt", "invalid", "Use ISO-8601 format, e.g. 2024-01-01T12:00:00Z");
            return null;
        }
    }

    private Map<String, Object> parseVariables(String variables, BindingResult bindingResult) {
        if (!StringUtils.hasText(variables)) return Collections.emptyMap();

        try {
            return objectMapper.readValue(variables, new TypeReference<>() {});
        } catch (Exception ex) {
            bindingResult.rejectValue("variables", "invalid", "Must be valid JSON");
            return Collections.emptyMap();
        }
    }

    private void validateContentChoice(NotificationForm form, BindingResult bindingResult) {
        boolean hasSubject = StringUtils.hasText(form.getSubject());
        boolean hasTemplate = StringUtils.hasText(form.getTemplateCode());

        if (!hasSubject && !hasTemplate) {
            bindingResult.reject("content.required", "Provide either subject or template code");
        }
    }
}
