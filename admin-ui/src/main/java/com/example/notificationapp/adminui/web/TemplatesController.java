package com.example.notificationapp.adminui.web;

import com.example.notificationapp.adminui.api.ApiClient;
import com.example.notificationapp.adminui.api.ApiProblemException;
import com.example.notificationapp.adminui.api.ApiProblemMapper;
import com.example.notificationapp.adminui.model.ApiProblemAlert;
import com.example.notificationapp.adminui.model.TemplateDetail;
import com.example.notificationapp.adminui.model.TemplatePage;
import com.example.notificationapp.adminui.web.form.TemplateCreateForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/templates")
public class TemplatesController {

    private final ApiClient apiClient;

    public TemplatesController(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @GetMapping
    public String list(Model model, HttpServletRequest request) {
        if (model.containsAttribute("templateCode")) {
            model.addAttribute("problemMessage", "Template created");
        }
        if (model.containsAttribute("deactivated")) {
            model.addAttribute("problemMessage", "Template deactivated");
        }
        try {
            TemplatePage page = apiClient.getTemplates(null);
            model.addAttribute("page", page);
        } catch (ApiProblemException exception) {
            ApiProblemAlert alert = ApiProblemMapper.toAlert(exception);
            model.addAttribute("problem", alert);
        }
        if (isHx(request)) {
            return "templates/list :: table";
        }
        return "templates/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        if (!model.containsAttribute("templateForm")) {
            model.addAttribute("templateForm", new TemplateCreateForm());
        }
        return "templates/create";
    }

    @PostMapping
    public String create(
            @ModelAttribute("templateForm") @Valid TemplateCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "templates/create";
        }
        try {
            TemplateDetail created = apiClient.createTemplate(form.toRequest());
            redirectAttributes.addFlashAttribute("templateCode", created.code());
            return "redirect:/templates";
        } catch (ApiProblemException exception) {
            ApiProblemAlert alert = ApiProblemMapper.toAlert(exception);
            model.addAttribute("problem", alert);
            return "templates/create";
        }
    }

    @GetMapping("/{code}")
    public String detail(@PathVariable String code, Model model) {
        try {
            TemplateDetail template = apiClient.getTemplate(code);
            model.addAttribute("template", template);
            return "templates/detail";
        } catch (ApiProblemException exception) {
            ApiProblemAlert alert = ApiProblemMapper.toAlert(exception);
            model.addAttribute("problem", alert);
            return "templates/detail";
        }
    }

    @PostMapping("/{code}/deactivate")
    public String deactivate(@PathVariable String code, RedirectAttributes redirectAttributes) {
        try {
            apiClient.deactivateTemplate(code);
            redirectAttributes.addFlashAttribute("deactivated", code);
            return "redirect:/templates";
        } catch (ApiProblemException exception) {
            ApiProblemAlert alert = ApiProblemMapper.toAlert(exception);
            redirectAttributes.addFlashAttribute("problem", alert);
            return "redirect:/templates";
        }
    }

    private boolean isHx(HttpServletRequest request) {
        return request.getHeader("HX-Request") != null;
    }
}
