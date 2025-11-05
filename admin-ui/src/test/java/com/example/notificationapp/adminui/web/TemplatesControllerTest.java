package com.example.notificationapp.adminui.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.notificationapp.adminui.api.ApiClient;
import com.example.notificationapp.adminui.api.ApiProblemException;
import com.example.notificationapp.adminui.model.TemplateDetail;
import com.example.notificationapp.adminui.model.TemplatePage;
import com.example.notificationapp.adminui.model.TemplateSummary;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TemplatesController.class)
class TemplatesControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ApiClient apiClient;

    @Test
    void rendersTemplatesList() throws Exception {
        TemplateSummary summary = new TemplateSummary("welcome", "ACTIVE", OffsetDateTime.now());
        TemplatePage page = new TemplatePage(List.of(summary), 0, 20, 1, 1, true, true);
        when(apiClient.getTemplates(any())).thenReturn(page);

        mockMvc
                .perform(get("/templates"))
                .andExpect(status().isOk())
                .andExpect(view().name("templates/list"))
                .andExpect(model().attributeExists("page"));
    }

    @Test
    void createTemplateRedirectsToList() throws Exception {
        TemplateDetail detail =
                new TemplateDetail(
                        "welcome",
                        "ACTIVE",
                        "Hello",
                        "<p>Hello</p>",
                        "Hello",
                        OffsetDateTime.now(),
                        OffsetDateTime.now());
        when(apiClient.createTemplate(any())).thenReturn(detail);

        mockMvc
                .perform(
                        post("/templates")
                                .param("code", "welcome")
                                .param("subject", "Hello")
                                .param("bodyHtml", "<p>Hello</p>")
                                .param("bodyText", "Hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));
    }

    @Test
    void deactivateTemplateRedirects() throws Exception {
        TemplateDetail detail =
                new TemplateDetail(
                        "welcome",
                        "INACTIVE",
                        "Hello",
                        "<p>Hello</p>",
                        "Hello",
                        OffsetDateTime.now(),
                        OffsetDateTime.now());
        when(apiClient.deactivateTemplate(eq("welcome"))).thenReturn(detail);

        mockMvc
                .perform(post("/templates/welcome/deactivate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));

        verify(apiClient).deactivateTemplate("welcome");
    }

    @Test
    void surfacesProblemDetailOnListError() throws Exception {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiProblemException exception =
            new ApiProblemException(problem, HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders());
        when(apiClient.getTemplates(any())).thenThrow(exception);

        mockMvc.perform(get("/templates"))
            .andExpect(status().isOk())
            .andExpect(view().name("templates/list"))
            .andExpect(model().attributeExists("problem"));
    }
}
