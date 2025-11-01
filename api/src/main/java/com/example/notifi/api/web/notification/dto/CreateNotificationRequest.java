package com.example.notifi.api.web.notification.dto;

import com.example.notifi.common.model.Channel;
import com.example.notifi.api.validation.ExternalRequestId;
import com.example.notifi.api.validation.SendAtWindow;
import com.example.notifi.api.validation.TemplateCode;
import com.example.notifi.api.validation.XorFields;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Map;

@XorFields(first = "subject", second = "templateCode")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateNotificationRequest {

    @ExternalRequestId
    private String externalRequestId;

    @NotNull
    private Channel channel = Channel.EMAIL;

    @NotBlank
    @Email
    @Size(max = 254)
    private String to;

    @Size(min = 1, max = 998)
    private String subject;

    @TemplateCode
    private String templateCode;

    private Map<String, Object> variables;

    @SendAtWindow
    private Instant sendAt;

    public CreateNotificationRequest() {}

    public String getExternalRequestId() { return externalRequestId; }
    public void setExternalRequestId(String externalRequestId) { this.externalRequestId = externalRequestId; }

    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }

    public Instant getSendAt() { return sendAt; }
    public void setSendAt(Instant sendAt) { this.sendAt = sendAt; }
}
