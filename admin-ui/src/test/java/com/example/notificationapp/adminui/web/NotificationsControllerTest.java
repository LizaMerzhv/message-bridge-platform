package com.example.notificationapp.adminui.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.notificationapp.adminui.api.ApiClient;
import com.example.notificationapp.adminui.api.ApiProblemException;
import com.example.notificationapp.adminui.model.DeliveryAttempt;
import com.example.notificationapp.adminui.model.NotificationDetail;
import com.example.notificationapp.adminui.model.NotificationPage;
import com.example.notificationapp.adminui.model.NotificationSummary;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationsController.class)
class NotificationsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ApiClient apiClient;

    @Test
    void rendersNotificationsList() throws Exception {
        NotificationSummary summary =
                new NotificationSummary("id-1", "user@example.com", "SENT", OffsetDateTime.now(), OffsetDateTime.now(), 1);
        NotificationPage page = new NotificationPage(List.of(summary), 0, 20, 1, 1, true, true);
        when(apiClient.getNotifications(any())).thenReturn(page);

        mockMvc
                .perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/list"))
                .andExpect(model().attributeExists("page"));
    }

    @Test
    void showsProblemDetailsOnFailure() throws Exception {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiProblemException exception =
            new ApiProblemException(problem, HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders());
        when(apiClient.getNotifications(any())).thenThrow(exception);

        mockMvc.perform(get("/notifications"))
            .andExpect(status().isOk())
            .andExpect(view().name("notifications/list"))
            .andExpect(model().attributeExists("problem"));
    }

    @Test
    void rejectsWhenSubjectAndTemplateProvided() throws Exception {
        mockMvc
                .perform(
                        post("/notifications")
                                .param("to", "user@example.com")
                                .param("externalRequestId", "req-1")
                                .param("subject", "Hello")
                                .param("templateCode", "welcome"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/create"))
                .andExpect(model().attributeHasErrors("notificationForm"));
    }

    @Test
    void redirectsToDetailAfterCreation() throws Exception {
        NotificationDetail detail =
                new NotificationDetail(
                        "notif-1",
                        "email",
                        "user@example.com",
                        "SENT",
                        "Hello",
                        null,
                        Map.of(),
                        "req-1",
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        1,
                        List.of(new DeliveryAttempt(1, "SENT", OffsetDateTime.now(), "email", "user@example.com", "Hello", null, null)));
        when(apiClient.createNotification(any())).thenReturn(detail);

        mockMvc
                .perform(
                        post("/notifications")
                                .param("to", "user@example.com")
                                .param("externalRequestId", "req-1")
                                .param("subject", "Hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications/notif-1"));
    }
}
