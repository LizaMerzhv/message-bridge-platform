package com.example.notifi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.notifi.api.test.WebTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class InternalServiceAuthFilterTest extends WebTestBase {

  private InternalServiceAuthFilter filter;
  private MockMvc mvc;

  @RestController
  static class DummyInternalController {
    @PostMapping("/internal/test")
    public String internal() {
      return "ok";
    }
  }

  @BeforeEach
  void setUpMvc() throws Exception {
    this.filter =
        new InternalServiceAuthFilter(
            new ObjectMapper().findAndRegisterModules(), "internal-shared-secret");
    this.filter.init(new MockFilterConfig());

    this.mvc =
        MockMvcBuilders.standaloneSetup(new DummyInternalController())
            .addFilters(new RequestIdFilter(), filter)
            .build();
  }

  @Test
  void shouldReturn401WhenInternalHeaderMissing() throws Exception {
    var result = mvc.perform(post("/internal/test")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(result.getResponse().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  @Test
  void shouldReturn401WhenInternalHeaderInvalid() throws Exception {
    var result =
        mvc.perform(
                post("/internal/test")
                    .header(InternalServiceAuthFilter.INTERNAL_AUTH_HEADER, "wrong-secret"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void shouldPassWhenInternalHeaderValid() throws Exception {
    var result =
        mvc.perform(
                post("/internal/test")
                    .header(
                        InternalServiceAuthFilter.INTERNAL_AUTH_HEADER,
                        "internal-shared-secret"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
  }
}
