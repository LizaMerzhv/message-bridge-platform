package com.example.notifi.api.web.template;

import com.example.notifi.api.core.template.TemplateCreateCommand;
import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.core.template.TemplateView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateCreateRequest request) {
        TemplateCreateCommand command = new TemplateCreateCommand();
        command.setCode(request.getCode());
        command.setSubject(request.getSubject());
        command.setBodyHtml(request.getBodyHtml());
        command.setBodyText(request.getBodyText());
        TemplateView created = templateService.create(command);
        return ResponseEntity.created(URI.create(String.format("/api/v1/templates/%s", created.getId())))
                .body(new TemplateResponse(created));
    }

    @GetMapping
    public List<TemplateResponse> list() {
        return templateService.list().stream().map(TemplateResponse::new).toList();
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@PathVariable UUID id) {
        return new TemplateResponse(templateService.get(id));
    }

    @PostMapping("/{id}/deactivate")
    public TemplateResponse deactivate(@PathVariable UUID id) {
        return new TemplateResponse(templateService.deactivate(id));
    }
}
