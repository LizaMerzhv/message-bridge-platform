package com.example.notifi.adminservice.dto;

public class TemplateCreateRequest {
  private String code;
  private String subject;
  private String bodyHtml;
  private String bodyText;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
  public String getBodyHtml() { return bodyHtml; }
  public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
  public String getBodyText() { return bodyText; }
  public void setBodyText(String bodyText) { this.bodyText = bodyText; }
}
