package com.example.notifi.api.dto;

import com.example.notifi.api.web.notification.dto.CreateNotificationRequest;
import com.example.notifi.api.data.entity.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateNotificationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory vf = Validation.byProvider(HibernateValidator.class)
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
        validator = vf.getValidator();
    }

    private CreateNotificationRequest base() {
        CreateNotificationRequest r = new CreateNotificationRequest();
        r.setExternalRequestId("REQ-1");
        r.setChannel(Channel.EMAIL);
        r.setTo("user@example.com");
        r.setVariables(Map.of("name", "Alice"));
        r.setSendAt(Instant.now().plusSeconds(60));
        return r;
    }

    @Test
    void valid_with_subject_or_template_only() {
        CreateNotificationRequest r1 = base();
        r1.setSubject("Hello");
        assertThat(violations(r1)).isEmpty();

        CreateNotificationRequest r2 = base();
        r2.setTemplateCode("WELCOME_2025");
        assertThat(violations(r2)).isEmpty();
    }

    @Test
    void invalid_when_both_subject_and_template() {
        CreateNotificationRequest r = base();
        r.setSubject("Hi");
        r.setTemplateCode("WELCOME_2025");
        assertThat(violations(r)).isNotEmpty();
    }

    @Test
    void invalid_when_none_subject_nor_template() {
        CreateNotificationRequest r = base();
        assertThat(violations(r)).isNotEmpty();
    }

    @Test
    void invalid_email_and_missing_externalRequestId() {
        CreateNotificationRequest r = base();
        r.setSubject("s");
        r.setTo("bad");
        r.setExternalRequestId(null);
        assertThat(violations(r)).extracting(v -> v.getPropertyPath().toString())
            .contains("to", "externalRequestId");
    }

    @Test
    void invalid_sendAt_past() {
        CreateNotificationRequest r = base();
        r.setSubject("s");
        r.setSendAt(Instant.now().minusSeconds(1));
        assertThat(violations(r)).isNotEmpty();
    }

    private Set<ConstraintViolation<CreateNotificationRequest>> violations(CreateNotificationRequest r) {
        return validator.validate(r);
    }
}
