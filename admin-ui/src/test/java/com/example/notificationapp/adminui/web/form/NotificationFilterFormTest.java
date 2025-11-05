package com.example.notificationapp.adminui.web.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

class NotificationFilterFormTest {

    @Test
    void convertsLocalDateTimesToUtcStrings() {
        NotificationFilterForm form = new NotificationFilterForm();
        form.setCreatedFrom("2024-03-01T10:15");
        form.setCreatedTo("2024-03-02T18:45");

        MultiValueMap<String, String> params = form.toQueryParams();

        assertThat(params.get("createdFrom")).isEqualTo(List.of("2024-03-01T10:15Z"));
        assertThat(params.get("createdTo")).isEqualTo(List.of("2024-03-02T18:45Z"));
    }

    @Test
    void ignoresBlankFields() {
        NotificationFilterForm form = new NotificationFilterForm();
        form.setStatus(" ");
        form.setTo(null);

        MultiValueMap<String, String> params = form.toQueryParams();

        assertThat(params.containsKey("status")).isFalse();
        assertThat(params.containsKey("to")).isFalse();
    }
}
