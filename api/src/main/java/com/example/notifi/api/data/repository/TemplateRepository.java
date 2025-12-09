package com.example.notifi.api.data.repository;

import com.example.notifi.api.data.entity.TemplateEntity;
import com.example.notifi.api.data.entity.TemplateStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TemplateRepository
    extends JpaRepository<TemplateEntity, UUID>, JpaSpecificationExecutor<TemplateEntity> {

  Optional<TemplateEntity> findByCode(String code);

  List<TemplateEntity> findAllByStatus(TemplateStatus status);
}
