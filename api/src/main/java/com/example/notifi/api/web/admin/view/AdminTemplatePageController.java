package com.example.notifi.api.web.admin.view;

import com.example.notifi.api.core.template.TemplateCreateCommand;
import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.core.template.TemplateView;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/admin/ui/templates")
public class AdminTemplatePageController {

    private final TemplateService templateService;

    public AdminTemplatePageController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public String list(@PageableDefault(size = 20) Pageable pageable, Model model) {
        Page<TemplateView> page = templateService.findAll(pageable);
        model.addAttribute("page", page);
        return "admin/templates";
    }

    @GetMapping("/{code}")
    public String detail(@PathVariable String code, Model model) {
        TemplateView template = templateService.getByCode(code);
        model.addAttribute("template", template);
        return "admin/template-detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("templateForm")) {
            model.addAttribute("templateForm", new TemplateForm());
        }
        return "admin/template-new";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("templateForm") TemplateForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/template-new";
        }

        TemplateCreateCommand command = new TemplateCreateCommand();
        command.setCode(form.getCode());
        command.setSubject(form.getSubject());
        command.setBodyHtml(form.getBodyHtml());
        command.setBodyText(form.getBodyText());

        TemplateView created = templateService.create(command);
        redirectAttributes.addFlashAttribute("created", true);
        return "redirect:/admin/ui/templates/" + created.getCode();
    }
}
