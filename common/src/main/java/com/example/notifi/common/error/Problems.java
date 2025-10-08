package com.example.notifi.common.error;

import java.net.URI;

public final class Problems {
    private Problems() {}

    private static ProblemDetails basic(int status, String title, String detail, String instancePath) {
        URI type = URI.create("about:blank");
        URI instance = instancePath != null ? URI.create(instancePath) : null;
        return ProblemDetails.of(type, title, status, detail, instance);
    }

    public static ProblemDetails badRequest(String detail) {
        return basic(400, "Bad Request", detail, null);
    }
    public static ProblemDetails unauthorized(String detail) {
        return basic(401, "Unauthorized", detail, null);
    }
    public static ProblemDetails forbidden(String detail) {
        return basic(403, "Forbidden", detail, null);
    }
    public static ProblemDetails notFound(String detail) {
        return basic(404, "Not Found", detail, null);
    }
    public static ProblemDetails conflict(String detail) {
        return basic(409, "Conflict", detail, null);
    }
    public static ProblemDetails unprocessable(String detail) {
        return basic(422, "Unprocessable Entity", detail, null);
    }
}
