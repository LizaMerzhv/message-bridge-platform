package com.example.notifi.api.data.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEntity {

  @Id private UUID id;

  @Column(name = "\"messageKey\"", nullable = false, length = 128)
  private String messageKey;

  @Column(name = "\"eventType\"", nullable = false, length = 64)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OutboxStatus status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "\"lastAttemptAt\"")
  private Instant lastAttemptAt;

  @Column(name = "\"publishedAt\"")
  private Instant publishedAt;

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

  public String getMessageKey() {
    return messageKey;
  }

  public void setMessageKey(String messageKey) {
    this.messageKey = messageKey;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxStatus status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  public void setLastAttemptAt(Instant lastAttemptAt) {
    this.lastAttemptAt = lastAttemptAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
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
