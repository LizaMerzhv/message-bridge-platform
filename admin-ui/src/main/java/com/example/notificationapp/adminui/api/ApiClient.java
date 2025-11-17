package com.example.notificationapp.adminui.api;

import com.example.notificationapp.adminui.config.AdminUiProperties;
import com.example.notificationapp.adminui.model.NotificationDetail;
import com.example.notificationapp.adminui.model.NotificationPage;
import com.example.notificationapp.adminui.model.TemplateDetail;
import com.example.notificationapp.adminui.model.TemplatePage;
import java.util.LinkedHashMap;
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
    private final AdminUiProperties properties;

    public ApiClient(RestTemplate restTemplate, AdminUiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public NotificationPage getNotifications(@Nullable MultiValueMap<String, String> params) {
        String uri = buildUri("/admin/notifications", params);
        return restTemplate.getForObject(uri, NotificationPage.class);
    }

    public NotificationDetail getNotification(String id) {
        return restTemplate.getForObject("/admin/notifications/{id}", NotificationDetail.class, id);
    }

    public NotificationDetail createNotification(Map<String, Object> body) {
        Map<String, Object> payload = new LinkedHashMap<>(body);
        payload.putIfAbsent("clientId", properties.clientId());
        return restTemplate.postForObject("/admin/notifications", payload, NotificationDetail.class);
    }

    public TemplatePage getTemplates(@Nullable MultiValueMap<String, String> params) {
        String uri = buildUri("/admin/templates", params);
        return restTemplate.getForObject(uri, TemplatePage.class);
    }

    public TemplateDetail createTemplate(Map<String, Object> body) {
        return restTemplate.postForObject("/admin/templates", body, TemplateDetail.class);
    }

    public TemplateDetail getTemplate(String code) {
        return restTemplate.getForObject("/admin/templates/{code}", TemplateDetail.class, code);
    }

    public TemplateDetail deactivateTemplate(String code) {
        return restTemplate.postForObject("/admin/templates/{code}/deactivate", null, TemplateDetail.class, code);
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
