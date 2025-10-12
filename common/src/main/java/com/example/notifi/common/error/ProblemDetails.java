package com.example.notifi.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class ProblemDetails {
    private URI type;
    private String title;
    private Integer status;
    private String detail;
    private URI instance;

    private String traceId;
    private String errorCode;
    private Map<String, java.util.List<String>> errors;

    public URI getType() { return type; }
    public void setType(URI type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public URI getInstance() { return instance; }
    public void setInstance(URI instance) { this.instance = instance; }

    @JsonProperty("traceId")
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Map<String, List<String>> getErrors() { return errors; }
    public void setErrors(Map<String, List<String>> errors) {
        this.errors = (errors == null || errors.isEmpty()) ? null : errors;
    }
}
