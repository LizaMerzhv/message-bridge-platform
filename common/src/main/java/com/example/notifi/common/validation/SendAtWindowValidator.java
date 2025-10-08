package com.example.notifi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Duration;
import java.time.Instant;

public class SendAtWindowValidator implements ConstraintValidator<SendAtWindow, Instant> {
    private long maxDays;

    @Override
    public void initialize(SendAtWindow constraintAnnotation) {
        this.maxDays = constraintAnnotation.maxDays();
    }

    @Override
    public boolean isValid(Instant value, ConstraintValidatorContext context) {
        if (value == null) return true;
        Instant now = Instant.now();
        Instant max = now.plus(Duration.ofDays(maxDays));
        return !value.isBefore(now) && (value.equals(max) || value.isBefore(max));
    }
}
