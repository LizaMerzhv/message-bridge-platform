package com.example.notifi.api.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.notifi.api.test.WebTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ProblemDetailsAdviceTest extends WebTestBase {

  private MockMvc mvc;

  @RestController
  static class BoomController {
    @GetMapping("/bad")
    String bad() {
      throw new IllegalArgumentException("oops");
    }
  }

  @BeforeEach
  void setUpMvc() {
    this.mvc =
        MockMvcBuilders.standaloneSetup(new BoomController())
            .setControllerAdvice(new ProblemDetailsAdvice())
            .build();
  }

  @Test
  void should_return_problem_json_on_bad_request() throws Exception {
    var result = mvc.perform(get("/bad")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(result.getResponse().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    ProblemDetails p =
        om.readValue(result.getResponse().getContentAsByteArray(), ProblemDetails.class);
    assertThat(p.getStatus()).isEqualTo(400);
    assertThat(p.getTitle()).isEqualTo("Bad Request");
    assertThat(p.getDetail()).contains("oops");
    assertThat(p.getTraceId()).isEqualTo("test-trace");
  }
}
