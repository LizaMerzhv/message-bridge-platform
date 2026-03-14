package com.example.notifi.api.core.notification;

import com.example.notifi.api.core.notification.exceptions.NotificationNotFoundException;
import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.data.entity.ClientEntity;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.data.repository.NotificationDeliveryAttemptEntity;
import com.example.notifi.api.data.repository.NotificationDeliveryAttemptRepository;
import com.example.notifi.api.data.repository.NotificationRepository;
import com.example.notifi.api.data.specifications.NotificationSpecifications;
import com.example.notifi.api.model.Channel;
import com.example.notifi.api.model.DeliveryStatus;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final ClientRepository clientRepository;
  private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;
  private final NotificationMapper notificationMapper;
  private final NotificationPolicy notificationPolicy;
  private final TemplateService templateService;
  private final NotificationTaskPublisher taskPublisher;
  private final Clock clock;

  public NotificationService(
      NotificationRepository notificationRepository,
      ClientRepository clientRepository,
      NotificationDeliveryAttemptRepository deliveryAttemptRepository,
      NotificationMapper notificationMapper,
      NotificationPolicy notificationPolicy,
      TemplateService templateService,
      NotificationTaskPublisher taskPublisher,
      Clock clock) {
    this.notificationRepository = notificationRepository;
    this.clientRepository = clientRepository;
    this.deliveryAttemptRepository = deliveryAttemptRepository;
    this.notificationMapper = notificationMapper;
    this.notificationPolicy = notificationPolicy;
    this.templateService = templateService;
    this.taskPublisher = taskPublisher;
    this.clock = clock;
  }

  public NotificationEntity recordDeliveryResult(
      UUID id,
      NotificationStatus status,
      int attempt,
      Instant attemptedAt,
      String errorCode,
      String errorMessage) {
    if (status != NotificationStatus.SENT && status != NotificationStatus.FAILED) {
      throw new IllegalArgumentException("Unsupported status: " + status);
    }

    if (attempt < 1) {
      throw new IllegalArgumentException("Attempt must be positive");
    }

    NotificationEntity entity =
        notificationRepository
            .findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));

    Instant occurredAt = attemptedAt != null ? attemptedAt : clock.instant();
    NotificationDeliveryAttemptEntity attemptEntity =
        deliveryAttemptRepository.findByNotificationIdAndAttempt(id, attempt).orElse(null);

    if (attemptEntity != null) {
      if (!attemptEntity.getOccurredAt().isAfter(occurredAt)) {
        attemptEntity.setStatus(toDeliveryStatus(status));
        attemptEntity.setErrorCode(errorCode);
        attemptEntity.setErrorMessage(truncate(errorMessage));
        attemptEntity.setOccurredAt(occurredAt);
        deliveryAttemptRepository.save(attemptEntity);
      }
    } else {
      NotificationDeliveryAttemptEntity newAttempt = new NotificationDeliveryAttemptEntity();
      newAttempt.setId(UUID.randomUUID());
      newAttempt.setNotificationId(id);
      newAttempt.setAttempt(attempt);
      newAttempt.setStatus(toDeliveryStatus(status));
      newAttempt.setErrorCode(errorCode);
      newAttempt.setErrorMessage(truncate(errorMessage));
      newAttempt.setOccurredAt(occurredAt);
      deliveryAttemptRepository.save(newAttempt);
    }

    entity.setAttempts(Math.max(entity.getAttempts(), attempt));
    if (status == NotificationStatus.SENT
        || (status == NotificationStatus.FAILED && entity.getStatus() != NotificationStatus.SENT)) {
      entity.setStatus(status);
    }
    if (entity.getUpdatedAt() == null || entity.getUpdatedAt().isBefore(occurredAt)) {
      entity.setUpdatedAt(occurredAt);
    }

    return notificationRepository.save(entity);
  }

  public CreateNotificationResult create(
      CreateNotificationRequest request, ClientPrincipal principal) {
    if (request.getChannel() != Channel.EMAIL) {
      throw new IllegalArgumentException("Only email channel is supported");
    }

    notificationPolicy.validateSendAt(request.getSendAt(), clock);

    NotificationEntity existing =
        notificationRepository
            .findByClientIdAndExternalRequestId(
                principal.clientId(), request.getExternalRequestId())
            .orElse(null);
    if (existing != null) {
      return new CreateNotificationResult(existing, true);
    }

    if (request.getTemplateCode() != null) {
      templateService.requireActiveByCode(request.getTemplateCode());
    }

    Instant now = clock.instant();

    NotificationEntity entity = new NotificationEntity();
    entity.setId(UUID.randomUUID());
    entity.setClientId(principal.clientId());
    entity.setChannel(request.getChannel().toValue());
    entity.setTo(request.getTo());
    entity.setSubject(request.getSubject());
    entity.setTemplateCode(request.getTemplateCode());
    entity.setVariables(request.getVariables());
    entity.setExternalRequestId(request.getExternalRequestId());
    entity.setSendAt(request.getSendAt() != null ? request.getSendAt() : now);
    entity.setStatus(NotificationStatus.CREATED);
    entity.setAttempts(0);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    NotificationEntity saved = notificationRepository.save(entity);

    ClientEntity client =
        clientRepository
            .findById(principal.clientId())
            .orElseThrow(
                () -> new IllegalStateException("Client not found: " + principal.clientId()));

    taskPublisher.publish(saved, client);

    return new CreateNotificationResult(saved, false);
  }

  @Transactional(readOnly = true)
  public NotificationView findByIdForClient(UUID id, UUID clientId) {
    NotificationEntity entity =
        notificationRepository
            .findByIdAndClientId(id, clientId)
            .orElseThrow(() -> new NotificationNotFoundException(id));
    return notificationMapper.toView(
        entity, deliveryAttemptRepository.findByNotificationIdOrderByAttemptAsc(entity.getId()));
  }

  @Transactional(readOnly = true)
  public Page<NotificationView> findAllForClient(
      NotificationFilter filter, UUID clientId, Pageable pageable) {
    Specification<NotificationEntity> spec =
        Specification.where(NotificationSpecifications.belongsToClient(clientId))
            .and(NotificationSpecifications.hasStatus(filter.getStatus()))
            .and(NotificationSpecifications.createdAtFrom(filter.getCreatedFrom()))
            .and(NotificationSpecifications.createdAtTo(filter.getCreatedTo()));
    return notificationRepository.findAll(spec, pageable).map(notificationMapper::toView);
  }

  @Transactional(readOnly = true)
  public Page<NotificationView> findAll(NotificationFilter filter, Pageable pageable) {
    Specification<NotificationEntity> spec =
        Specification.where(NotificationSpecifications.hasClient(filter.getClientId()))
            .and(NotificationSpecifications.hasStatus(filter.getStatus()))
            .and(NotificationSpecifications.createdAtFrom(filter.getCreatedFrom()))
            .and(NotificationSpecifications.createdAtTo(filter.getCreatedTo()));
    return notificationRepository.findAll(spec, pageable).map(notificationMapper::toView);
  }

  @Transactional(readOnly = true)
  public NotificationView findById(UUID id) {
    NotificationEntity entity =
        notificationRepository
            .findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));
    return notificationMapper.toView(
        entity, deliveryAttemptRepository.findByNotificationIdOrderByAttemptAsc(entity.getId()));
  }

  @Transactional(readOnly = true)
  public List<DeliveryView> findDeliveries(UUID id) {
    NotificationEntity entity =
        notificationRepository
            .findById(id)
            .orElseThrow(() -> new NotificationNotFoundException(id));
    return notificationMapper.toDeliveryViews(
        deliveryAttemptRepository.findByNotificationIdOrderByAttemptAsc(entity.getId()));
  }

  @Transactional(readOnly = true)
  public long count(NotificationFilter filter) {
    Specification<NotificationEntity> spec = buildSpecification(filter, filter.getStatus());
    return notificationRepository.count(spec);
  }

  @Transactional(readOnly = true)
  public long countByStatus(NotificationFilter filter, NotificationStatus status) {
    Specification<NotificationEntity> spec = buildSpecification(filter, status);
    return notificationRepository.count(spec);
  }

  private Specification<NotificationEntity> buildSpecification(
      NotificationFilter filter, NotificationStatus statusOverride) {
    return Specification.where(NotificationSpecifications.hasClient(filter.getClientId()))
        .and(NotificationSpecifications.hasStatus(statusOverride))
        .and(NotificationSpecifications.createdAtFrom(filter.getCreatedFrom()))
        .and(NotificationSpecifications.createdAtTo(filter.getCreatedTo()));
  }

  private DeliveryStatus toDeliveryStatus(NotificationStatus status) {
    return status == NotificationStatus.SENT ? DeliveryStatus.SENT : DeliveryStatus.FAILED;
  }

  private String truncate(String value) {
    if (value == null) {
      return null;
    }
    int maxLength = 1024;
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
