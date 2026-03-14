package com.example.notifi.securityservice.data.entity;

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

  @Column(name = "\"rateLimitPerMin\"", nullable = false)
  private Integer rateLimitPerMin;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getApiKey() {
    return apiKey;
  }

  public Integer getRateLimitPerMin() {
    return rateLimitPerMin;
  }
}
