package com.example.notifi.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SendAtWindowValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SendAtWindow {
  String message() default "sendAt must be in [now, now + 365 days]";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  long maxDays() default 365;
}
