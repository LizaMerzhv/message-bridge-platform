package com.example.notifi.api.web.admin.template.dto;

import com.example.notifi.api.data.entity.TemplateEntity;
import com.example.notifi.api.data.entity.TemplateStatus;
import com.example.notifi.api.web.admin.template.dto.TemplateCreateRequest;
import com.example.notifi.api.web.admin.template.dto.TemplateDetailDto;
import com.example.notifi.api.web.admin.template.dto.TemplateSummaryDto;
import com.example.notifi.api.web.admin.template.dto.UpdateTemplateRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TemplateAdminMapper {

    public TemplateSummaryDto toSummary(TemplateEntity entity) {
        return new TemplateSummaryDto()
            .setId(entity.getId())
            .setCode(entity.getCode())
            .setSubject(entity.getSubject())
            .setStatus(entity.getStatus().name())
            .setCreatedAt(entity.getCreatedAt())
            .setUpdatedAt(entity.getUpdatedAt());
    }

    public TemplateDetailDto toDetail(TemplateEntity entity) {
        return new TemplateDetailDto()
            .setId(entity.getId())
            .setCode(entity.getCode())
            .setSubject(entity.getSubject())
            .setBodyHtml(entity.getBodyHtml())
            .setBodyText(entity.getBodyText())
            .setStatus(entity.getStatus().name())
            .setCreatedAt(entity.getCreatedAt())
            .setUpdatedAt(entity.getUpdatedAt());
    }

    public void apply(TemplateCreateRequest request, TemplateEntity entity) {
        entity.setCode(request.getCode());
        entity.setSubject(request.getSubject());
        entity.setBodyHtml(request.getBodyHtml());
        entity.setBodyText(request.getBodyText());
        entity.setStatus(resolveStatus(request.getStatus(), TemplateStatus.ACTIVE));
    }

    public void apply(UpdateTemplateRequest request, TemplateEntity entity) {
        if (StringUtils.hasText(request.getSubject())) {
            entity.setSubject(request.getSubject());
        }
        if (request.getBodyHtml() != null) {
            entity.setBodyHtml(request.getBodyHtml());
        }
        if (request.getBodyText() != null) {
            entity.setBodyText(request.getBodyText());
        }
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(resolveStatus(request.getStatus(), entity.getStatus()));
        }
    }

    private TemplateStatus resolveStatus(String status, TemplateStatus fallback) {
        if (!StringUtils.hasText(status)) {
            return fallback;
        }
        return TemplateStatus.valueOf(status.toUpperCase());
    }
}
