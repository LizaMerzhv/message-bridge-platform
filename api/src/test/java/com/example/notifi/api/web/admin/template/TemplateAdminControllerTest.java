package com.example.notifi.api.web.admin.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notifi.api.core.template.TemplateService;
import com.example.notifi.api.core.template.TemplateView;
import com.example.notifi.api.core.template.exception.TemplateCodeNotFoundException;
import com.example.notifi.api.data.entity.TemplateStatus;
import com.example.notifi.api.web.admin.template.dto.TemplateCreateRequest;
import com.example.notifi.api.web.error.ProblemDetailsAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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

        TemplateAdminController controller = new TemplateAdminController(templateService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new ProblemDetailsAdvice())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()) // ВАЖНО
            .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private TemplateView buildView(TemplateStatus status) {
        TemplateView view = new TemplateView();
        view.setId(UUID.randomUUID());
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
        TemplateView view = buildView(TemplateStatus.ACTIVE);
        when(templateService.create(any())).thenReturn(view);

        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setCode("WELCOME");
        request.setSubject("Subject");
        request.setBodyHtml("<p>Hello</p>");

        mockMvc.perform(post("/admin/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/admin/templates/" + view.getCode()))
            .andExpect(jsonPath("$.code").value("WELCOME"));
    }

    @Test
    void create_ShouldReturnBadRequest_WhenCodeInvalid() throws Exception {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setCode("lowercase");
        request.setSubject("Subject");

        mockMvc.perform(post("/admin/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.traceId").value("test-trace"));
    }

    @Test
    void list_ShouldReturnPagedResponse() throws Exception {
        TemplateView view = buildView(TemplateStatus.ACTIVE);

        when(templateService.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(view), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/admin/templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].code").value("WELCOME"));
    }

    @Test
    void deactivate_ShouldReturnInactive() throws Exception {
        TemplateView view = buildView(TemplateStatus.INACTIVE);
        when(templateService.deactivateByCode("WELCOME")).thenReturn(view);

        mockMvc.perform(post("/admin/templates/{code}/deactivate", "WELCOME"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void get_ShouldReturnProblem_WhenTemplateMissing() throws Exception {
        when(templateService.getByCode("UNKNOWN"))
            .thenThrow(new TemplateCodeNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/admin/templates/{code}", "UNKNOWN"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.traceId").value("test-trace"));
    }
}
