package com.example.notifi.api.core.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notifi.api.core.notification.NotificationService;
import com.example.notifi.api.core.notification.exceptions.NotificationNotFoundException;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.web.error.ProblemDetailsAdvice;
import com.example.notifi.api.web.internal.notification.NotificationInternalController;
import com.example.notifi.api.web.internal.notification.NotificationDeliveryUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class NotificationInternalControllerTest {

    private MockMvc mockMvc;
    private NotificationService notificationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.notificationService = Mockito.mock(NotificationService.class);
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        var controller = new NotificationInternalController(notificationService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new ProblemDetailsAdvice())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();
    }

    @Test
    void updateStatus_ShouldReturn204_WhenNotificationExists() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationDeliveryUpdateRequest request = new NotificationDeliveryUpdateRequest();
        request.setStatus(NotificationStatus.SENT);
        request.setAttemptedAt(Instant.parse("2024-01-01T00:00:05Z"));

        mockMvc.perform(post("/internal/notifications/{id}/deliveries", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(notificationService)
            .recordDeliveryResult(id, NotificationStatus.SENT, request.getAttemptedAt(), null);
    }

    @Test
    void updateStatus_ShouldReturn404_WhenNotificationMissing() throws Exception {
        UUID id = UUID.randomUUID();
        NotificationDeliveryUpdateRequest request = new NotificationDeliveryUpdateRequest();
        request.setStatus(NotificationStatus.SENT);
        request.setAttemptedAt(Instant.parse("2024-01-01T00:00:05Z"));

        doThrow(new NotificationNotFoundException(id))
            .when(notificationService)
            .recordDeliveryResult(any(), any(), any(), any());

        mockMvc.perform(post("/internal/notifications/{id}/deliveries", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }
}
