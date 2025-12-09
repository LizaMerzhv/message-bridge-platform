package com.example.notifi.api.web.admin.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.data.entity.TemplateEntity;
import com.example.notifi.api.data.entity.TemplateStatus;
import com.example.notifi.api.web.admin.template.dto.TemplateAdminMapper;
import com.example.notifi.api.web.error.ProblemDetailsAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class TemplateAdminControllerTest {

  private MockMvc mockMvc;
  private TemplateService templateService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MDC.put("traceId", "test-trace");

    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.templateService = Mockito.mock(TemplateService.class);

    TemplateAdminController controller =
        new TemplateAdminController(templateService, new TemplateAdminMapper());

    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
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
  void createTemplate_shouldReturnCreated() throws Exception {
    TemplateEntity entity = buildEntity(TemplateStatus.ACTIVE);
    when(templateService.createTemplate(any())).thenReturn(entity);

    String body =
        "{"
            + "\"code\":\"WELCOME\","
            + "\"subject\":\"Subject\","
            + "\"bodyHtml\":\"<p>Hello</p>\""
            + "}";

    mockMvc
        .perform(post("/admin/templates").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/admin/templates/" + entity.getId()))
        .andExpect(jsonPath("$.id").value(entity.getId().toString()))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void getTemplate_shouldReturnDetails() throws Exception {
    TemplateEntity entity = buildEntity(TemplateStatus.INACTIVE);
    when(templateService.findByIdOrThrow(entity.getId())).thenReturn(entity);

    mockMvc
        .perform(get("/admin/templates/{id}", entity.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(entity.getId().toString()))
        .andExpect(jsonPath("$.code").value("WELCOME"))
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  private TemplateEntity buildEntity(TemplateStatus status) {
    TemplateEntity entity = new TemplateEntity();
    entity.setId(UUID.randomUUID());
    entity.setCode("WELCOME");
    entity.setSubject("Subject");
    entity.setBodyHtml("<p>Hello</p>");
    entity.setBodyText("Hello");
    entity.setStatus(status);
    entity.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    entity.setUpdatedAt(Instant.parse("2024-01-02T00:00:00Z"));
    return entity;
  }
}
