package com.example.notifi.common.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;

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
    void serializes_full_and_basic_factories() throws JsonProcessingException {
        ProblemDetails p = ProblemDetails.of(
                URI.create("about:blank"),
                "Unprocessable Entity",
                422,
                "detail",
                URI.create("/api/v1/resource")
        );
        String json = om.writeValueAsString(p);
        assertThat(json).contains("\"type\":\"about:blank\"");
        assertThat(json).contains("\"title\":\"Unprocessable Entity\"");
        assertThat(json).contains("\"status\":422");
        assertThat(json).contains("\"detail\":\"detail\"");
        assertThat(json).contains("\"instance\":\"/api/v1/resource\"");

        assertThat(Problems.badRequest("x").getStatus()).isEqualTo(400);
        assertThat(Problems.conflict("y").getStatus()).isEqualTo(409);
        assertThat(Problems.unprocessable("z").getStatus()).isEqualTo(422);
    }
}