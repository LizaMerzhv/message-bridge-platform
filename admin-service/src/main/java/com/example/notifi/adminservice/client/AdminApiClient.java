package com.example.notifi.adminservice.client;

import com.example.notifi.adminservice.dto.*;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AdminApiClient {

  private static final String INTERNAL_AUTH_HEADER = "X-Internal-Auth";

  private final RestClient restClient;

  public AdminApiClient(
      RestClient.Builder builder,
      @Value("${admin.api.base-url:http://localhost:8080}") String baseUrl,
      @Value("${admin.api.internal-shared-secret:notifi-internal-dev-secret}")
          String sharedSecret) {
    this.restClient =
        builder
            .baseUrl(baseUrl)
            .defaultHeader(INTERNAL_AUTH_HEADER, sharedSecret)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  public PageResponse<NotificationSummaryDto> listNotifications(
      String status, String clientId, int page, int size) {
    return restClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/internal/admin/v1/notifications")
                    .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
                    .queryParamIfPresent("clientId", java.util.Optional.ofNullable(clientId))
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public NotificationDetailDto getNotification(UUID id) {
    return restClient
        .get()
        .uri("/internal/admin/v1/notifications/{id}", id)
        .retrieve()
        .body(NotificationDetailDto.class);
  }

  public List<DeliveryAttemptDto> getNotificationAttempts(UUID id) {
    return restClient
        .get()
        .uri("/internal/admin/v1/notifications/{id}/attempts", id)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public NotificationDetailDto createNotification(AdminCreateNotificationRequest request) {
    return restClient
        .post()
        .uri("/internal/admin/v1/notifications")
        .body(request)
        .retrieve()
        .body(NotificationDetailDto.class);
  }

  public PageResponse<TemplateSummaryDto> listTemplates(int page, int size) {
    return restClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/internal/admin/v1/templates")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public TemplateDetailDto getTemplateByCode(String code) {
    return restClient
        .get()
        .uri("/internal/admin/v1/templates/by-code/{code}", code)
        .retrieve()
        .body(TemplateDetailDto.class);
  }

  public TemplateDetailDto createTemplate(TemplateCreateRequest request) {
    return restClient
        .post()
        .uri("/internal/admin/v1/templates")
        .body(request)
        .retrieve()
        .body(TemplateDetailDto.class);
  }

  public TemplateDetailDto deactivateTemplate(String code) {
    return restClient
        .post()
        .uri("/internal/admin/v1/templates/by-code/{code}/deactivate", code)
        .retrieve()
        .body(TemplateDetailDto.class);
  }
}
