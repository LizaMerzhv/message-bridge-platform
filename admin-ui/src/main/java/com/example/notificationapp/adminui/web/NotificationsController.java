package com.example.notificationapp.adminui.web;

import com.example.notificationapp.adminui.api.ApiClient;
import com.example.notificationapp.adminui.api.ApiProblemException;
import com.example.notificationapp.adminui.api.ApiProblemMapper;
import com.example.notificationapp.adminui.model.ApiProblemAlert;
import com.example.notificationapp.adminui.model.NotificationDetail;
import com.example.notificationapp.adminui.model.NotificationPage;
import com.example.notificationapp.adminui.web.form.NotificationCreateForm;
import com.example.notificationapp.adminui.web.form.NotificationFilterForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationsController {

    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;

    public NotificationsController(ApiClient apiClient, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
    }

    @ModelAttribute("notificationStatuses")
    public List<String> notificationStatuses() {
        return List.of("QUEUED", "SENT", "FAILED", "CANCELLED");
    }

    @GetMapping
    public String list(
        @ModelAttribute("filters") NotificationFilterForm filters,
        Model model,
        HttpServletRequest request) {
        try {
            NotificationPage page = apiClient.getNotifications(filters.toQueryParams());
            model.addAttribute("page", page);
        } catch (ApiProblemException exception) {
            ApiProblemAlert alert = ApiProblemMapper.toAlert(exception);
            model.addAttribute("problem", alert);
        }
        if (isHx(request)) {
            return "notifications/list :: table";
        }
        return "notifications/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        if (model.containsAttribute("createdId")) {
            model.addAttribute("problemMessage", "Notification created successfully");
        }
        try {
            NotificationDetail notification = apiClient.getNotification(id);
            model.addAttribute("notification", notification);
            return "notifications/detail";
        } catch (ApiProblemException exception) {
            ApiProblemAlert alert = ApiProblemMapper.toAlert(exception);
            model.addAttribute("problem", alert);
            return "notifications/detail";
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        if (!model.containsAttribute("notificationForm")) {
            model.addAttribute("notificationForm", new NotificationCreateForm());
        }
        return "notifications/create";
    }

    @PostMapping
    public String create(
        @ModelAttribute("notificationForm") @Valid NotificationCreateForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes) {

        // subject XOR templateCode
        if (StringUtils.hasText(form.getSubject()) && StringUtils.hasText(form.getTemplateCode())) {
            bindingResult.rejectValue("subject", "subject.template.exclusive",
                "Either 'subject' or 'templateCode' must be provided, not both");
            bindingResult.rejectValue("templateCode", "subject.template.exclusive",
                "Either 'subject' or 'templateCode' must be provided, not both");
        }
        if (!StringUtils.hasText(form.getSubject()) && !StringUtils.hasText(form.getTemplateCode())) {
            bindingResult.reject("subject.template.required",
                "Provide either 'subject' (with text) or 'templateCode'");
        }

        // variables JSON
        Map<String, Object> variablesObject = new LinkedHashMap<>();
        if (StringUtils.hasText(form.getVariables())) {
            try {
                variablesObject = objectMapper.readValue(
                    form.getVariables(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                bindingResult.rejectValue("variables", "variables.invalid", "Invalid JSON");
            }
        }

        // sendAt
        Optional<OffsetDateTime> sendAt = form.sendAtUtc();
        if (StringUtils.hasText(form.getSendAt()) && sendAt.isEmpty()) {
            bindingResult.rejectValue("sendAt", "sendAt.invalid", "Invalid datetime");
        }

        if (bindingResult.hasErrors()) {
            return "notifications/create";
        }

        try {
            NotificationDetail created = apiClient.createNotification(form.toRequest(variablesObject, sendAt));
            if (created == null || created.id() == null) {
                model.addAttribute(
                    "problem",
                    new ApiProblemAlert(
                        "about:blank",                        // type
                        "Invalid API response",               // title
                        null,                                 // detail
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), // status
                        Map.of(),                             // parameters
                        Map.of()                              // headers
                    )
                );         return "notifications/create";
            }
            redirectAttributes.addFlashAttribute("createdId", created.id());
            return "redirect:/notifications/" + created.id();
        } catch (ApiProblemException exception) {
            ApiProblemAlert alert = ApiProblemMapper.toAlert(exception);
            model.addAttribute("problem", alert);
            return "notifications/create";
        }
    }

    private boolean isHx(HttpServletRequest request) {
        return request.getHeader("HX-Request") != null;
    }
}
