package com.example.notifi.adminservice.web;

import com.example.notifi.adminservice.client.AdminApiClient;
import com.example.notifi.adminservice.dto.TemplateCreateRequest;
import com.example.notifi.adminservice.dto.TemplateDetailDto;
import com.example.notifi.adminservice.dto.TemplateSummaryDto;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ui/templates")
public class AdminTemplatePageController {

  private final AdminApiClient adminApiClient;

  public AdminTemplatePageController(AdminApiClient adminApiClient) {
    this.adminApiClient = adminApiClient;
  }

  @GetMapping
  public String list(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      Model model) {
    model.addAttribute("page", new UiPage<TemplateSummaryDto>(adminApiClient.listTemplates(page, size)));
    model.addAttribute("activePage", "templates");
    return "admin/templates";
  }

  @GetMapping("/{code}")
  public String detail(@PathVariable String code, Model model) {
    model.addAttribute("template", adminApiClient.getTemplateByCode(code));
    model.addAttribute("activePage", "templates");
    return "admin/template-detail";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    if (!model.containsAttribute("templateForm")) model.addAttribute("templateForm", new TemplateForm());
    model.addAttribute("activePage", "templates");
    return "admin/template-new";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("templateForm") TemplateForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("activePage", "templates");
      return "admin/template-new";
    }
    TemplateCreateRequest request = new TemplateCreateRequest();
    request.setCode(form.getCode());
    request.setSubject(form.getSubject());
    request.setBodyHtml(form.getBodyHtml());
    request.setBodyText(form.getBodyText());
    TemplateDetailDto created = adminApiClient.createTemplate(request);
    redirectAttributes.addFlashAttribute("created", true);
    return "redirect:/admin/ui/templates/" + created.getCode();
  }

  @PostMapping("/{code}/deactivate")
  public String deactivate(@PathVariable String code, RedirectAttributes redirectAttributes) {
    TemplateDetailDto updated = adminApiClient.deactivateTemplate(code);
    redirectAttributes.addFlashAttribute("deactivated", true);
    return "redirect:/admin/ui/templates/" + updated.getCode();
  }
}
