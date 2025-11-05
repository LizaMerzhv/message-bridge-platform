package com.example.notificationapp.adminui.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.StringUtils;

public class SubjectTemplateXorValidator implements ConstraintValidator<SubjectTemplateXor, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object subject = wrapper.getPropertyValue("subject");
        Object templateCode = wrapper.getPropertyValue("templateCode");
        boolean hasSubject = subject instanceof String && StringUtils.hasText((String) subject);
        boolean hasTemplate = templateCode instanceof String && StringUtils.hasText((String) templateCode);
        return hasSubject ^ hasTemplate;
    }
}
