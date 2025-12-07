package com.example.notifi.api.core.template;

import com.example.notifi.api.core.template.exceptions.TemplateCodeNotFoundException;
import com.example.notifi.api.core.template.exceptions.TemplateInactiveException;
import com.example.notifi.api.core.template.exceptions.TemplateNotFoundException;
import com.example.notifi.api.data.entity.TemplateEntity;
import com.example.notifi.api.data.entity.TemplateStatus;
import com.example.notifi.api.data.repository.TemplateRepository;
import com.example.notifi.api.web.admin.template.dto.TemplateAdminMapper;
import com.example.notifi.api.web.admin.template.dto.TemplateCreateRequest;
import com.example.notifi.api.web.admin.template.dto.UpdateTemplateRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;
    private final TemplateAdminMapper adminMapper;
    private final Clock clock;

    public TemplateService(
        TemplateRepository templateRepository,
        TemplateMapper templateMapper,
        TemplateAdminMapper adminMapper,
        Clock clock) {
        this.templateRepository = templateRepository;
        this.templateMapper = templateMapper;
        this.adminMapper = adminMapper;
        this.clock = clock;
    }

    public TemplateView create(TemplateCreateCommand command) {
        TemplateEntity saved =
            templateRepository.save(
                createEntity(
                    command.getCode(),
                    command.getSubject(),
                    command.getBodyHtml(),
                    command.getBodyText(),
                    TemplateStatus.ACTIVE));
        return templateMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public TemplateView get(UUID id) {
        TemplateEntity entity =
            templateRepository.findById(id).orElseThrow(() -> new TemplateNotFoundException(id));
        return templateMapper.toView(entity);
    }

    @Transactional(readOnly = true)
    public TemplateView getByCode(String code) {
        TemplateEntity entity =
            templateRepository
                .findByCode(code)
                .orElseThrow(() -> new TemplateCodeNotFoundException(code));
        return templateMapper.toView(entity);
    }

    @Transactional(readOnly = true)
    public Page<TemplateView> findAll(Pageable pageable) {
        return templateRepository.findAll(pageable).map(templateMapper::toView);
    }

    public TemplateView deactivateByCode(String code) {
        TemplateEntity entity =
            templateRepository
                .findByCode(code)
                .orElseThrow(() -> new TemplateCodeNotFoundException(code));
        if (entity.getStatus() == TemplateStatus.INACTIVE) {
            return templateMapper.toView(entity);
        }
        entity.setStatus(TemplateStatus.INACTIVE);
        entity.setUpdatedAt(clock.instant());
        return templateMapper.toView(templateRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public TemplateEntity requireActiveByCode(String code) {
        TemplateEntity entity =
            templateRepository
                .findByCode(code)
                .orElseThrow(() -> new TemplateCodeNotFoundException(code));
        if (entity.getStatus() != TemplateStatus.ACTIVE) {
            throw new TemplateInactiveException(code);
        }
        return entity;
    }

    /**
     * Админский лист с фильтрами по статусу и коду (contains).
     * Возвращаем сущности, маппинг в DTO делается на уровне контроллера.
     */
    @Transactional(readOnly = true)
    public Page<TemplateEntity> findAll(String status, String code, Pageable pageable) {
        Specification<TemplateEntity> specification = Specification.where(null);

        if (StringUtils.hasText(status)) {
            TemplateStatus templateStatus = TemplateStatus.valueOf(status.toUpperCase());
            specification =
                specification.and((root, query, cb) -> cb.equal(root.get("status"), templateStatus));
        }

        if (StringUtils.hasText(code)) {
            String pattern = "%" + code.toLowerCase() + "%";
            specification =
                specification.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("code")), pattern));
        }

        return templateRepository.findAll(specification, pageable);
    }

    public TemplateEntity findByIdOrThrow(UUID id) {
        return templateRepository.findById(id).orElseThrow(() -> new TemplateNotFoundException(id));
    }

    public TemplateEntity createTemplate(TemplateCreateRequest request) {
        TemplateEntity entity =
            createEntity(
                request.getCode(),
                request.getSubject(),
                request.getBodyHtml(),
                request.getBodyText(),
                null);
        adminMapper.apply(request, entity);
        return templateRepository.save(entity);
    }

    public TemplateEntity updateTemplate(UUID id, UpdateTemplateRequest request) {
        TemplateEntity entity = findByIdOrThrow(id);
        adminMapper.apply(request, entity);
        entity.setUpdatedAt(clock.instant());
        return templateRepository.save(entity);
    }

    public TemplateEntity activate(UUID id) {
        TemplateEntity entity = findByIdOrThrow(id);
        entity.setStatus(TemplateStatus.ACTIVE);
        entity.setUpdatedAt(clock.instant());
        return templateRepository.save(entity);
    }

    public TemplateEntity deactivate(UUID id) {
        TemplateEntity entity = findByIdOrThrow(id);
        entity.setStatus(TemplateStatus.INACTIVE);
        entity.setUpdatedAt(clock.instant());
        return templateRepository.save(entity);
    }

    private TemplateEntity createEntity(
        String code, String subject, String bodyHtml, String bodyText, TemplateStatus status) {
        Instant now = clock.instant();
        TemplateEntity entity = new TemplateEntity();
        entity.setId(UUID.randomUUID());
        entity.setCode(code);
        entity.setSubject(subject);
        entity.setBodyHtml(bodyHtml);
        entity.setBodyText(bodyText);
        entity.setStatus(status != null ? status : TemplateStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
