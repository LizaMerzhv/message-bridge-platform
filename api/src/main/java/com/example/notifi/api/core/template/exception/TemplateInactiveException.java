package com.example.notifi.api.core.template.exception;

public class TemplateInactiveException extends RuntimeException {
    private final String code;

    public TemplateInactiveException(String code) {
        super("Template is inactive: " + code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
