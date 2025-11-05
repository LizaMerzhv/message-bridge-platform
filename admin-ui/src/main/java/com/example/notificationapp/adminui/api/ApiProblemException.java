package com.example.notificationapp.adminui.api;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public class ApiProblemException extends RuntimeException {

    private final ProblemDetail problem;
    private final HttpStatusCode statusCode;
    private final HttpHeaders headers;
    private final Map<String, String> headerSnapshot;

    public ApiProblemException(ProblemDetail problem, HttpStatusCode statusCode, HttpHeaders headers) {
        super(problem != null ? problem.getDetail() : null);
        this.problem = problem;
        this.statusCode = statusCode;
        this.headers = headers;
        this.headerSnapshot = headers != null ? headers.toSingleValueMap() : Map.of();
    }

    public ProblemDetail problem() {
        return problem;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public Map<String, String> headerSnapshot() {
        return headerSnapshot;
    }
}
