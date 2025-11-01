package com.example.notifi.api.error;

import com.example.notifi.api.web.error.ProblemDetails;
import com.example.notifi.api.web.error.Problems;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProblemDetailsJsonTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void excludes_nulls() throws JsonProcessingException {
        ProblemDetails p = new ProblemDetails();
        p.setTitle("Bad Request");
        p.setStatus(400);
        String json = om.writeValueAsString(p);
        assertThat(json).isEqualTo("{\"title\":\"Bad Request\",\"status\":400}");
    }

    @Test
    void problem_serializes_as_problem_json() throws Exception {
        ProblemDetails p = Problems.notFound("N/A", "/r", "t");
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules();
        String json = om.writeValueAsString(p);
        assertThat(json).contains("\"status\":404");
        assertThat(json).doesNotContain("\"errors\":{}"); // пустое -> null
    }

}
