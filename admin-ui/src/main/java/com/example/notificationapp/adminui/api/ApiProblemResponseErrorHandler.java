package com.example.notificationapp.adminui.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

public class ApiProblemResponseErrorHandler implements ResponseErrorHandler {

    private final ObjectMapper objectMapper;

    public ApiProblemResponseErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        throw toProblem(response);
    }

    @Override
    public void handleError(
            java.net.URI url, org.springframework.http.HttpMethod method, ClientHttpResponse response)
            throws IOException {
        throw toProblem(response);
    }

    private ApiProblemException toProblem(ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        HttpHeaders headers = response.getHeaders();
        ProblemDetail problem = null;
        try {
            if (response.getBody() != null) {
                byte[] bytes = response.getBody().readAllBytes();
                if (bytes.length > 0) {
                    problem = objectMapper.readValue(bytes, ProblemDetail.class);
                }
            }
        } catch (IOException ex) {
            problem = ProblemDetail.forStatus(status);
            problem.setDetail(ex.getMessage());
        }
        if (problem == null) {
            problem = ProblemDetail.forStatus(status);
            problem.setDetail(response.getStatusText());
        }
        return new ApiProblemException(problem, status, headers);
    }
}
