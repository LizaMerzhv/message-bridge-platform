package com.example.notifi.api.web.admin.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notifi.api.core.notification.NotificationFilter;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.data.repository.ClientRepository;
import com.example.notifi.api.web.error.ProblemDetailsAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class NotificationAdminControllerTest {

  private MockMvc mockMvc;
  private NotificationService notificationService;
  private ClientRepository clientRepository;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MDC.put("traceId", "test-trace");

    this.notificationService = Mockito.mock(NotificationService.class);
    this.clientRepository = Mockito.mock(ClientRepository.class);
    this.objectMapper = new ObjectMapper().findAndRegisterModules();

    NotificationAdminController controller =
        new NotificationAdminController(
            notificationService, clientRepository, new NotificationAdminMapper());

    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new ProblemDetailsAdvice())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void list_ShouldApplyFiltersAndReturnResults() throws Exception {
    NotificationView view = view();
    Page<NotificationView> page = new PageImpl<>(List.of(view), PageRequest.of(0, 20), 1);
    Mockito.when(notificationService.findAll(any(NotificationFilter.class), any(PageRequest.class)))
        .thenReturn(page);

    mockMvc
        .perform(
            get("/admin/notifications")
                .param("status", "sent")
                .param("clientId", view.getClientId().toString())
                .param("createdFrom", "2024-01-01T00:00:00Z")
                .param("createdTo", "2024-01-02T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(view.getId().toString()))
        .andExpect(jsonPath("$.content[0].status").value("SENT"));

    ArgumentCaptor<NotificationFilter> filterCaptor =
        ArgumentCaptor.forClass(NotificationFilter.class);
    ArgumentCaptor<PageRequest> pageableCaptor = ArgumentCaptor.forClass(PageRequest.class);

    Mockito.verify(notificationService).findAll(filterCaptor.capture(), pageableCaptor.capture());

    NotificationFilter applied = filterCaptor.getValue();
    assertThat(applied.getStatus()).isEqualTo(NotificationStatus.SENT);
    assertThat(applied.getClientId()).isEqualTo(view.getClientId());
    assertThat(applied.getCreatedFrom()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    assertThat(applied.getCreatedTo()).isEqualTo(Instant.parse("2024-01-02T00:00:00Z"));
    assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
  }

  @Test
  void getById_ShouldReturnDetailWithDeliveries() throws Exception {
    NotificationView view = view();
    view.setDeliveries(List.of(deliveryView()));
    Mockito.when(notificationService.findById(view.getId())).thenReturn(view);

    mockMvc
        .perform(get("/admin/notifications/{id}", view.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(view.getId().toString()))
        .andExpect(jsonPath("$.deliveries[0].status").value("SENT"));
  }

  private NotificationView view() {
    NotificationView view = new NotificationView();
    view.setId(UUID.randomUUID());
    view.setClientId(UUID.randomUUID());
    view.setChannel("EMAIL");
    view.setTo("user@example.com");
    view.setSubject("Subject");
    view.setTemplateCode("WELCOME");
    view.setVariables(Map.of("name", "User"));
    view.setExternalRequestId("ext-1");
    view.setSendAt(Instant.parse("2024-01-01T00:00:00Z"));
    view.setSendAtEffective(view.getSendAt());
    view.setStatus(NotificationStatus.SENT);
    view.setAttempts(2);
    view.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    view.setUpdatedAt(Instant.parse("2024-01-01T00:00:01Z"));
    return view;
  }

  private com.example.notifi.api.core.notification.DeliveryView deliveryView() {
    com.example.notifi.api.core.notification.DeliveryView view =
        new com.example.notifi.api.core.notification.DeliveryView();
    view.setAttempt(1);
    view.setStatus(com.example.notifi.common.model.DeliveryStatus.SENT);
    view.setErrorCode(null);
    view.setErrorMessage(null);
    view.setOccurredAt(Instant.parse("2024-01-01T00:00:05Z"));
    view.setCreatedAt(Instant.parse("2024-01-01T00:00:05Z"));
    view.setLastAttemptAt(Instant.parse("2024-01-01T00:00:05Z"));
    return view;
  }
}
