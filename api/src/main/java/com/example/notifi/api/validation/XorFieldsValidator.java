package com.example.notifi.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

public class XorFieldsValidator implements ConstraintValidator<XorFields, Object> {
    private String first;
    private String second;

    @Override
    public void initialize(XorFields constraintAnnotation) {
        this.first = constraintAnnotation.first();
        this.second = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            Object a = read(value, first);
            Object b = read(value, second);
            boolean hasA = a != null && !(a instanceof String s && s.isBlank());
            boolean hasB = b != null && !(b instanceof String s && s.isBlank());
            return hasA ^ hasB;
        } catch (Exception e) {
            return false;
        }
    }

    private Object read(Object bean, String field) throws Exception {
        String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
        Method m = bean.getClass().getMethod(getter);
        return m.invoke(bean);
    }
}
