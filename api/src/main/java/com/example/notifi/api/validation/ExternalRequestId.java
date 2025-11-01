package com.example.notifi.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ExternalRequestIdValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalRequestId {
    String message() default "externalRequestId must match [A-Za-z0-9._-]{1,64}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
