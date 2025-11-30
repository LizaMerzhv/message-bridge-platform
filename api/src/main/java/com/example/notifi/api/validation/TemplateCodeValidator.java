package com.example.notifi.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class TemplateCodeValidator implements ConstraintValidator<TemplateCode, String> {
  private static final Pattern P = Pattern.compile("^[A-Z0-9_.-]{3,64}$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) return true; // nullable
    return P.matcher(value).matches();
  }
}
