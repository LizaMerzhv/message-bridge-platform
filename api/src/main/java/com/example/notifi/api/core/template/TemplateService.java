package com.example.notifi.api.core.template;

import com.example.notifi.api.core.template.exception.TemplateCodeNotFoundException;
import com.example.notifi.api.core.template.exception.TemplateInactiveException;
import com.example.notifi.api.core.template.exception.TemplateNotFoundException;
import com.example.notifi.api.data.entity.TemplateEntity;
import com.example.notifi.api.data.entity.TemplateStatus;
import com.example.notifi.api.data.repository.TemplateRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;
    private final Clock clock;

    public TemplateService(TemplateRepository templateRepository, TemplateMapper templateMapper, Clock clock) {
        this.templateRepository = templateRepository;
        this.templateMapper = templateMapper;
        this.clock = clock;
    }

    public TemplateView create(TemplateCreateCommand command) {
        Instant now = clock.instant();
        TemplateEntity entity = new TemplateEntity();
        entity.setId(UUID.randomUUID());
        entity.setCode(command.getCode());
        entity.setSubject(command.getSubject());
        entity.setBodyHtml(command.getBodyHtml());
        entity.setBodyText(command.getBodyText());
        entity.setStatus(TemplateStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        TemplateEntity saved = templateRepository.save(entity);
        return templateMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public TemplateView get(UUID id) {
        TemplateEntity entity =
                templateRepository.findById(id).orElseThrow(() -> new TemplateNotFoundException(id));
        return templateMapper.toView(entity);
    }

    @Transactional(readOnly = true)
    public List<TemplateView> list() {
        return templateRepository.findAll().stream().map(templateMapper::toView).toList();
    }

    public TemplateView deactivate(UUID id) {
        TemplateEntity entity =
                templateRepository.findById(id).orElseThrow(() -> new TemplateNotFoundException(id));
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
}
