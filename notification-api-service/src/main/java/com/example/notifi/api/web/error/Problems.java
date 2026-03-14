package com.example.notifi.api.web.error;

import static com.example.notifi.api.web.error.ProblemTypes.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

public final class Problems {
  private Problems() {}

  public static ProblemDetails of(
      int status,
      String type,
      String title,
      String detail,
      String instance,
      String traceId,
      String errorCode,
      Map<String, List<String>> errors) {
    ProblemDetails p = new ProblemDetails();
    p.setStatus(status);
    p.setType(type != null ? URI.create(type) : null);
    p.setTitle(title);
    p.setDetail(detail);
    p.setInstance(instance != null ? URI.create(instance) : null);
    p.setTraceId(traceId);
    p.setErrorCode(errorCode);
    p.setErrors(errors);
    return p;
  }

  public static ProblemDetails badRequest(String detail, String instance, String traceId) {
    return of(400, BAD_REQUEST, "Bad Request", detail, instance, traceId, null, null);
  }

  public static ProblemDetails unauthorized(String detail, String instance, String traceId) {
    return of(401, UNAUTHORIZED, "Unauthorized", detail, instance, traceId, null, null);
  }

  public static ProblemDetails forbidden(String detail, String instance, String traceId) {
    return of(403, FORBIDDEN, "Forbidden", detail, instance, traceId, null, null);
  }

  public static ProblemDetails notFound(String detail, String instance, String traceId) {
    return of(404, NOT_FOUND, "Not Found", detail, instance, traceId, null, null);
  }

  public static ProblemDetails conflict(String detail, String instance, String traceId) {
    return of(409, CONFLICT, "Conflict", detail, instance, traceId, null, null);
  }

  public static ProblemDetails unprocessable(
      String detail, String instance, String traceId, Map<String, List<String>> errors) {
    return of(422, UNPROCESSABLE, "Unprocessable Entity", detail, instance, traceId, null, errors);
  }

  public static ProblemDetails tooManyRequests(String detail, String instance, String traceId) {
    return of(429, TOO_MANY_REQUESTS, "Too Many Requests", detail, instance, traceId, null, null);
  }

  public static ProblemDetails internal(String detail, String instance, String traceId) {
    return of(500, INTERNAL, "Internal Server Error", detail, instance, traceId, null, null);
  }
}
