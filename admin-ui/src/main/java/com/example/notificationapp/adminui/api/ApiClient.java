package com.example.notificationapp.adminui.api;

import com.example.notificationapp.adminui.model.NotificationDetail;
import com.example.notificationapp.adminui.model.NotificationPage;
import com.example.notificationapp.adminui.model.TemplateDetail;
import com.example.notificationapp.adminui.model.TemplatePage;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ApiClient {

    private final RestTemplate restTemplate;

    public ApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public NotificationPage getNotifications(@Nullable MultiValueMap<String, String> params) {
        String uri = buildUri("/notifications", params);
        return restTemplate.getForObject(uri, NotificationPage.class);
    }

    public NotificationDetail getNotification(String id) {
        return restTemplate.getForObject("/notifications/{id}", NotificationDetail.class, id);
    }

    public NotificationDetail createNotification(Map<String, Object> body) {
        return restTemplate.postForObject("/notifications", body, NotificationDetail.class);
    }

    public TemplatePage getTemplates(@Nullable MultiValueMap<String, String> params) {
        String uri = buildUri("/templates", params);
        return restTemplate.getForObject(uri, TemplatePage.class);
    }

    public TemplateDetail createTemplate(Map<String, Object> body) {
        return restTemplate.postForObject("/templates", body, TemplateDetail.class);
    }

    public TemplateDetail getTemplate(String id) {
        return restTemplate.getForObject("/templates/{id}", TemplateDetail.class, id);
    }

    public TemplateDetail deactivateTemplate(String code) {
        Map<String, Object> request = Map.of("code", code, "status", "INACTIVE");
        return restTemplate.postForObject("/templates", request, TemplateDetail.class);
    }

    private String buildUri(String path, @Nullable MultiValueMap<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (params != null) {
            params.forEach(
                    (key, values) -> {
                        if (values != null) {
                            values.stream()
                                    .filter(Objects::nonNull)
                                    .forEach(value -> builder.queryParam(key, value));
                        }
                    });
        }
        return builder.encode().build().toUriString();
    }
}
