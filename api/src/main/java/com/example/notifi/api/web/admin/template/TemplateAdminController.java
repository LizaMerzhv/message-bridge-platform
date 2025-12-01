package com.example.notifi.api.web.admin.template;

import com.example.notifi.api.core.template.TemplateCreateCommand;
import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.core.template.TemplateView;
import com.example.notifi.api.web.admin.dto.PageResponse;
import com.example.notifi.api.web.admin.template.dto.TemplateCreateRequest;
import com.example.notifi.api.web.admin.template.dto.TemplateDetailDto;
import com.example.notifi.api.web.admin.template.dto.TemplateSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/templates")
@Tag(name = "Admin Templates", description = "Administrative operations for templates")
public class TemplateAdminController {

  private final TemplateService templateService;

  public TemplateAdminController(TemplateService templateService) {
    this.templateService = templateService;
  }

  @PostMapping
  @Operation(summary = "Create template")
  public ResponseEntity<TemplateDetailDto> create(
      @Valid @RequestBody TemplateCreateRequest request) {
    TemplateCreateCommand command = new TemplateCreateCommand();
    command.setCode(request.getCode());
    command.setSubject(request.getSubject());
    command.setBodyHtml(request.getBodyHtml());
    command.setBodyText(request.getBodyText());
    TemplateView created = templateService.create(command);
    return ResponseEntity.created(
            URI.create(String.format("/admin/templates/%s", created.getCode())))
        .body(new TemplateDetailDto(created));
  }

  @GetMapping
  @Operation(summary = "List templates")
  public PageResponse<TemplateSummaryDto> list(Pageable pageable) {
    Page<TemplateSummaryDto> page = templateService.findAll(pageable).map(TemplateSummaryDto::new);
    return PageResponse.from(page);
  }

  @GetMapping("/{code}")
  @Operation(summary = "Get template by code")
  public TemplateDetailDto get(@Parameter(description = "Template code") @PathVariable String code) {
    return new TemplateDetailDto(templateService.getByCode(code));
  }

  @PostMapping("/{code}/deactivate")
  @Operation(summary = "Deactivate template")
  public TemplateDetailDto deactivate(
      @Parameter(description = "Template code") @PathVariable String code) {
    return new TemplateDetailDto(templateService.deactivateByCode(code));
  }
}
