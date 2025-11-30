package com.example.notifi.api.core.template.exceptions;

public class TemplateCodeNotFoundException extends RuntimeException {
  private final String code;

  public TemplateCodeNotFoundException(String code) {
    super("Template not found: " + code);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
