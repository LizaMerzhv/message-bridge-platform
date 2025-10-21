package com.example.notifi.api.web.template;

import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.core.template.TemplateView;
import com.example.notifi.api.core.template.exception.TemplateNotFoundException;
import com.example.notifi.api.data.entity.TemplateStatus;
import com.example.notifi.api.web.error.ProblemDetailsAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TemplateControllerTest {

    private MockMvc mockMvc;
    private TemplateService templateService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MDC.put("traceId", "test-trace");

        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.templateService = Mockito.mock(TemplateService.class);

        // Контроллер c замоканным сервисом
        TemplateController controller = new TemplateController(templateService);

        // Validator для @Valid
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new ProblemDetailsAdvice())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private TemplateView buildView(UUID id, TemplateStatus status) {
        TemplateView view = new TemplateView();
        view.setId(id);
        view.setCode("WELCOME");
        view.setSubject("Subject");
        view.setBodyHtml("<p>Hello</p>");
        view.setBodyText("Hello");
        view.setStatus(status);
        view.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        view.setUpdatedAt(Instant.parse("2024-01-02T00:00:00Z"));
        return view;
    }

    @Test
    void create_ShouldReturnCreated_WhenRequestValid() throws Exception {
        UUID id = UUID.randomUUID();
        when(templateService.create(any())).thenReturn(buildView(id, TemplateStatus.ACTIVE));

        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setCode("WELCOME");
        request.setSubject("Subject");
        request.setBodyHtml("<p>Hello</p>");

        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/templates/" + id))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.code").value("WELCOME"));
    }

    @Test
    void create_ShouldReturnBadRequest_WhenCodeInvalid() throws Exception {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setCode("lowercase"); // заведомо некорректно
        request.setSubject("Subject");

        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.type").value("/problems/bad-request"))
            .andExpect(jsonPath("$.traceId").value("test-trace"));
    }

    @Test
    void create_ShouldReturnBadRequest_WhenBodyTooLarge() throws Exception {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setCode("WELCOME");
        request.setSubject("Subject");
        request.setBodyText("a".repeat(262_145)); // > 262144

        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.traceId").value("test-trace"));
    }

    @Test
    void deactivate_ShouldReturnInactive_WhenTemplateActive() throws Exception {
        UUID id = UUID.randomUUID();
        when(templateService.deactivate(id)).thenReturn(buildView(id, TemplateStatus.INACTIVE));

        mockMvc.perform(post("/api/v1/templates/{id}/deactivate", id))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deactivate_ShouldReturnInactive_WhenAlreadyInactive() throws Exception {
        UUID id = UUID.randomUUID();
        when(templateService.deactivate(id)).thenReturn(buildView(id, TemplateStatus.INACTIVE));

        mockMvc.perform(post("/api/v1/templates/{id}/deactivate", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void get_ShouldReturnProblem_WhenTemplateMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(templateService.get(id)).thenThrow(new TemplateNotFoundException(id));

        mockMvc.perform(get("/api/v1/templates/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.type").value("/problems/template-not-found"))
            .andExpect(jsonPath("$.traceId").value("test-trace"));
    }
}
