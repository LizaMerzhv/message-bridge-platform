package com.example.notifi.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetails {
    @JsonProperty("type")
    private URI type;
    @JsonProperty("title")
    private String title;
    @JsonProperty("status")
    private Integer status;
    @JsonProperty("detail")
    private String detail;
    @JsonProperty("instance")
    private URI instance;

    public ProblemDetails() {}

    public ProblemDetails(URI type, String title, Integer status, String detail, URI instance) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }

    public static ProblemDetails of(URI type, String title, Integer status, String detail, URI instance) {
        return new ProblemDetails(type, title, status, detail, instance);
    }

    public URI getType() { return type; }
    public String getTitle() { return title; }
    public Integer getStatus() { return status; }
    public String getDetail() { return detail; }
    public URI getInstance() { return instance; }

    public void setType(URI type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setStatus(Integer status) { this.status = status; }
    public void setDetail(String detail) { this.detail = detail; }
    public void setInstance(URI instance) { this.instance = instance; }
}
