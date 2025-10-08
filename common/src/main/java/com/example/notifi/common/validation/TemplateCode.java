package com.example.notifi.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TemplateCodeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateCode {
    String message() default "templateCode must match [A-Z0-9_.-]{3,64}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
