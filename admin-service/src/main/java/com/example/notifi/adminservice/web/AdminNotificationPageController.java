package com.example.notifi.adminservice.web;

import com.example.notifi.adminservice.client.AdminApiClient;
import com.example.notifi.adminservice.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ui/notifications")
public class AdminNotificationPageController {
  private final AdminApiClient adminApiClient;
  private final ObjectMapper objectMapper;

  public AdminNotificationPageController(AdminApiClient adminApiClient, ObjectMapper objectMapper) {
    this.adminApiClient = adminApiClient;
    this.objectMapper = objectMapper;
  }

  @GetMapping
  public String list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "clientId", required = false) String clientId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      Model model) {
    PageResponse<NotificationSummaryDto> result =
        adminApiClient.listNotifications(status, clientId, page, size);

    long sentCount =
        result.getContent().stream().filter(it -> "SENT".equalsIgnoreCase(it.getStatus())).count();
    long failedCount =
        result.getContent().stream()
            .filter(it -> "FAILED".equalsIgnoreCase(it.getStatus()))
            .count();

    model.addAttribute("page", new UiPage<>(result));
    model.addAttribute("statusFilter", status == null ? "" : status);
    model.addAttribute("clientFilter", clientId);
    model.addAttribute(
        "availableStatuses",
        java.util.Arrays.stream(NotificationStatus.values()).map(Enum::name).toList());
    model.addAttribute("sentCount", sentCount);
    model.addAttribute("failedCount", failedCount);
    model.addAttribute("activePage", "notifications");
    return "admin/notifications";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    if (!model.containsAttribute("notificationForm")) {
      model.addAttribute("notificationForm", new NotificationForm());
    }
    model.addAttribute("activePage", "notifications");
    return "admin/notification-new";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("notificationForm") NotificationForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {

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
    request.setChannel(form.getChannel().toUpperCase());
    request.setTo(form.getTo());
    request.setSubject(StringUtils.hasText(form.getSubject()) ? form.getSubject() : null);
    request.setTemplateCode(
        StringUtils.hasText(form.getTemplateCode()) ? form.getTemplateCode() : null);
    request.setVariables(variables);
    request.setSendAt(sendAt);
    request.setExternalRequestId(form.getExternalRequestId());

    NotificationDetailDto created = adminApiClient.createNotification(request);
    redirectAttributes.addFlashAttribute("created", true);
    return "redirect:/admin/ui/notifications/" + created.getId();
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable UUID id, Model model) {
    model.addAttribute("notification", adminApiClient.getNotification(id));
    model.addAttribute("attempts", adminApiClient.getNotificationAttempts(id));
    model.addAttribute("activePage", "notifications");
    return "admin/notification-detail";
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
      bindingResult.rejectValue(
          "sendAt", "invalid", "Use ISO-8601 format, e.g. 2024-01-01T12:00:00Z");
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
    if (!StringUtils.hasText(form.getSubject()) && !StringUtils.hasText(form.getTemplateCode())) {
      bindingResult.reject("content.required", "Provide either subject or template code");
    }
  }
}
