package com.example.notifi.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ExternalRequestIdValidator implements ConstraintValidator<ExternalRequestId, String> {
    private static final Pattern P = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return P.matcher(value).matches();
    }
}
