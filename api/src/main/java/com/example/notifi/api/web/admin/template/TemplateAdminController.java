package com.example.notifi.api.web.admin.template;

import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.data.entity.TemplateEntity;
import com.example.notifi.api.web.admin.dto.PageResponse;
import com.example.notifi.api.web.admin.template.dto.*;
import com.example.notifi.api.web.admin.template.dto.TemplateCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/templates")
@Tag(name = "Admin Templates", description = "Administrative operations for templates")
public class TemplateAdminController {

  private final TemplateService templateService;
  private final TemplateAdminMapper mapper;

  public TemplateAdminController(TemplateService templateService, TemplateAdminMapper mapper) {
    this.templateService = templateService;
    this.mapper = mapper;
  }

  @GetMapping
  @Operation(summary = "List templates")
  public PageResponse<TemplateSummaryDto> list(
      @Parameter(description = "Filter by status", example = "ACTIVE")
          @RequestParam(value = "status", required = false)
          String status,
      @Parameter(description = "Filter by code (contains)")
          @RequestParam(value = "code", required = false)
          String code,
      Pageable pageable) {

    Page<TemplateEntity> page = templateService.findAll(status, code, pageable);
    return PageResponse.from(page.map(mapper::toSummary));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get template by id")
  public TemplateDetailDto get(@PathVariable UUID id) {
    return mapper.toDetail(templateService.findByIdOrThrow(id));
  }

  @PostMapping
  @Operation(summary = "Create template")
  public ResponseEntity<TemplateDetailDto> create(
      @Valid @RequestBody TemplateCreateRequest request) {

    TemplateEntity created = templateService.createTemplate(request);
    URI location = URI.create(String.format("/admin/templates/%s", created.getId()));
    return ResponseEntity.created(location).body(mapper.toDetail(created));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update template")
  public TemplateDetailDto update(
      @PathVariable UUID id, @Valid @RequestBody UpdateTemplateRequest request) {
    return mapper.toDetail(templateService.updateTemplate(id, request));
  }

  @PostMapping("/{id}/activate")
  @Operation(summary = "Activate template")
  public TemplateDetailDto activate(@PathVariable UUID id) {
    return mapper.toDetail(templateService.activate(id));
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Deactivate template")
  public TemplateDetailDto deactivate(@PathVariable UUID id) {
    return mapper.toDetail(templateService.deactivate(id));
  }
}
