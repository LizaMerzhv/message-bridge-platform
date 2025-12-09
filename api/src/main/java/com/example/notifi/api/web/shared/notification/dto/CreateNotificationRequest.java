package com.example.notifi.api.web.shared.notification.dto;

import com.example.notifi.api.validation.ExternalRequestId;
import com.example.notifi.api.validation.SendAtWindow;
import com.example.notifi.api.validation.TemplateCode;
import com.example.notifi.api.validation.XorFields;
import com.example.notifi.common.model.Channel;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

@XorFields(first = "subject", second = "templateCode")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification creation payload")
public class CreateNotificationRequest {

  @ExternalRequestId
  @Schema(description = "Idempotency key provided by the client", example = "email-req-123")
  private String externalRequestId;

  @NotNull
  @Schema(description = "Delivery channel", example = "EMAIL")
  private Channel channel = Channel.EMAIL;

  @NotBlank
  @Email
  @Size(max = 254)
  @Schema(description = "Recipient address", example = "user@example.com")
  private String to;

  @Size(min = 1, max = 998)
  @Schema(description = "Subject line when not using a stored template", example = "Welcome")
  private String subject;

  @TemplateCode
  @Schema(description = "Reference to a stored template", example = "welcome_email")
  private String templateCode;

  @Schema(description = "Template variables or inline placeholders")
  private Map<String, Object> variables;

  @SendAtWindow
  @Schema(description = "Optional scheduling timestamp in UTC", example = "2024-06-18T10:15:30Z")
  private Instant sendAt;

  public CreateNotificationRequest() {}

  public String getExternalRequestId() {
    return externalRequestId;
  }

  public void setExternalRequestId(String externalRequestId) {
    this.externalRequestId = externalRequestId;
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public void setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables;
  }

  public Instant getSendAt() {
    return sendAt;
  }

  public void setSendAt(Instant sendAt) {
    this.sendAt = sendAt;
  }
}
