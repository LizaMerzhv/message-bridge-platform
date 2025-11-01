package com.example.notifi.api.web.notification;

import com.example.notifi.api.core.notification.CreateNotificationResult;
import com.example.notifi.api.core.notification.NotificationFilter;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.security.ClientAuthenticationToken;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.error.ProblemDetailsAdvice;
import com.example.notifi.api.web.notification.dto.CreateNotificationRequest;
import com.example.notifi.api.data.entity.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTest {

    private MockMvc mockMvc;
    private NotificationService notificationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MDC.put("traceId", "test-trace");

        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.notificationService = Mockito.mock(NotificationService.class);

        var controller = new NotificationController(notificationService);

        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new ProblemDetailsAdvice())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    private void authenticate(UUID clientId, String name, int rpm) {
        ClientPrincipal principal = new ClientPrincipal(clientId, name, rpm);
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new ClientAuthenticationToken(principal));
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void create_ShouldReturn201_WhenValidImmediate() throws Exception {
        authenticate(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Client", 60);

        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");

        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setStatus(NotificationStatus.CREATED);
        entity.setSendAt(now);

        CreateNotificationResult result = mock(CreateNotificationResult.class);
        when(result.getEntity()).thenReturn(entity);
        when(result.isReplayed()).thenReturn(false);

        when(notificationService.create(any(CreateNotificationRequest.class), any(ClientPrincipal.class)))
            .thenReturn(result);

        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setExternalRequestId("ext-1");
        req.setChannel(Channel.EMAIL);
        req.setTo("user@example.com");
        req.setSubject("Hello");

        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/notifications/" + id))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.sendAtEffective").exists());
    }

    @Test
    void create_ShouldReturn200_Replayed() throws Exception {
        authenticate(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Client", 60);

        UUID id = UUID.randomUUID();
        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setStatus(NotificationStatus.CREATED);

        CreateNotificationResult result = mock(CreateNotificationResult.class);
        when(result.getEntity()).thenReturn(entity);
        when(result.isReplayed()).thenReturn(true);

        when(notificationService.create(any(CreateNotificationRequest.class), any(ClientPrincipal.class)))
            .thenReturn(result);

        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setExternalRequestId("ext-2");
        req.setChannel(Channel.EMAIL);
        req.setTo("user@example.com");
        req.setSubject("Hello");

        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Idempotency-Replayed", "true"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void list_ShouldReturn200_AndPassFiltersToService() throws Exception {
        authenticate(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Client", 60);

        NotificationView view = new NotificationView();
        view.setId(UUID.randomUUID());
        view.setStatus(NotificationStatus.CREATED);
        view.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));

        when(notificationService.findAllForClient(any(NotificationFilter.class), any(UUID.class), any()))
            .thenReturn(new PageImpl<>(List.of(view), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/notifications")
                .param("status", "created")
                .param("createdFrom", "2024-01-01T00:00:00Z")
                .param("createdTo", "2024-01-02T00:00:00Z")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        var captor = org.mockito.ArgumentCaptor.forClass(NotificationFilter.class);
        verify(notificationService).findAllForClient(captor.capture(), any(UUID.class), any());
        var f = captor.getValue();
        assertThat(f.getStatus()).isEqualTo(NotificationStatus.CREATED);
        assertThat(f.getCreatedFrom()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(f.getCreatedTo()).isEqualTo(Instant.parse("2024-01-02T00:00:00Z"));
    }

    @Test
    void list_ShouldReturn400_WhenClientIdParamPresent() throws Exception {
        authenticate(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Client", 60);

        mockMvc.perform(get("/api/v1/notifications")
                .param("clientId", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"));
    }

    @Test
    void list_ShouldReturn400_WhenPageSizeTooLarge() throws Exception {
        authenticate(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Client", 60);

        mockMvc.perform(get("/api/v1/notifications")
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"));
    }
}
