package com.example.notificationapp.adminui.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notificationapp.adminui.web.form.NotificationCreateForm;
import org.junit.jupiter.api.Test;

class SubjectTemplateXorValidatorTest {

    private final SubjectTemplateXorValidator validator = new SubjectTemplateXorValidator();

    @Test
    void validWhenOnlySubject() {
        NotificationCreateForm form = new NotificationCreateForm();
        form.setSubject("Hello");
        form.setTemplateCode(null);
        assertThat(validator.isValid(form, null)).isTrue();
    }

    @Test
    void validWhenOnlyTemplateCode() {
        NotificationCreateForm form = new NotificationCreateForm();
        form.setSubject(null);
        form.setTemplateCode("welcome");
        assertThat(validator.isValid(form, null)).isTrue();
    }

    @Test
    void invalidWhenBothProvided() {
        NotificationCreateForm form = new NotificationCreateForm();
        form.setSubject("Hello");
        form.setTemplateCode("welcome");
        assertThat(validator.isValid(form, null)).isFalse();
    }
}
