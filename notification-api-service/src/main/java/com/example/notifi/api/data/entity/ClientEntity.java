package com.example.notifi.api.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client")
public class ClientEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "\"apiKey\"", nullable = false, unique = true, length = 64)
  private String apiKey;

  @Column(name = "\"webhookUrl\"")
  private String webhookUrl;

  @Column(name = "\"webhookSecret\"")
  private String webhookSecret;

  @Column(name = "\"rateLimitPerMin\"", nullable = false)
  private Integer rateLimitPerMin;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getWebhookUrl() {
    return webhookUrl;
  }

  public void setWebhookUrl(String webhookUrl) {
    this.webhookUrl = webhookUrl;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public Integer getRateLimitPerMin() {
    return rateLimitPerMin;
  }

  public void setRateLimitPerMin(Integer rateLimitPerMin) {
    this.rateLimitPerMin = rateLimitPerMin;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
