package com.example.notifi.api.core.template;

import com.example.notifi.api.data.entity.TemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {

  public TemplateView toView(TemplateEntity entity) {
    TemplateView view = new TemplateView();
    view.setId(entity.getId());
    view.setCode(entity.getCode());
    view.setSubject(entity.getSubject());
    view.setBodyHtml(entity.getBodyHtml());
    view.setBodyText(entity.getBodyText());
    view.setStatus(entity.getStatus());
    view.setCreatedAt(entity.getCreatedAt());
    view.setUpdatedAt(entity.getUpdatedAt());
    return view;
  }
}
