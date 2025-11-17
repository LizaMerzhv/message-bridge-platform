package com.example.notifi.api.web.publicapi.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notifi.api.core.notification.CreateNotificationResult;
import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.data.entity.NotificationEntity;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.security.ClientAuthenticationToken;
import com.example.notifi.api.security.ClientPrincipal;
import com.example.notifi.api.web.error.ProblemDetailsAdvice;
import com.example.notifi.api.web.publicapi.notification.NotificationPublicController;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationRequest;
import com.example.notifi.common.model.Channel;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class NotificationPublicControllerTest {

    private MockMvc mockMvc;
    private NotificationService notificationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MDC.put("traceId", "test-trace");
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.notificationService = Mockito.mock(NotificationService.class);
        var controller = new NotificationPublicController(notificationService);
        var validator = new LocalValidatorFactoryBean();
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
                .andExpect(header().doesNotExist("X-Idempotency-Replayed"));
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
                .andExpect(header().string("X-Idempotency-Replayed", "true"));
    }
}
