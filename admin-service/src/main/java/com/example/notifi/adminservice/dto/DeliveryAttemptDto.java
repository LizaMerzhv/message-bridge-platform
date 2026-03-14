package com.example.notifi.adminservice.dto;

import java.time.Instant;

public class DeliveryAttemptDto {
  private int attempt;
  private String status;
  private String channel;
  private String to;
  private String subject;
  private String errorCode;
  private String errorMessage;
  private Instant timestamp;

  public int getAttempt() { return attempt; }
  public String getStatus() { return status; }
  public String getChannel() { return channel; }
  public String getTo() { return to; }
  public String getSubject() { return subject; }
  public String getErrorCode() { return errorCode; }
  public String getErrorMessage() { return errorMessage; }
  public Instant getTimestamp() { return timestamp; }
}
