package com.example.notifi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.notifi.api.test.WebTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiKeyAuthFilterTest extends WebTestBase {

  private ApiKeyAuthFilter filter;
  private MockMvc mvc;

  @RestController
  static class DummyController {
    @GetMapping("/api/v1/secure")
    public String secure() {
      return "ok";
    }
  }

  @BeforeEach
  void setUpMvc() throws Exception {
    this.filter =
        new ApiKeyAuthFilter(new ObjectMapper().findAndRegisterModules(), "shared-secret");
    this.filter.init(new MockFilterConfig());

    this.mvc =
        MockMvcBuilders.standaloneSetup(new DummyController())
            .addFilters(new RequestIdFilter(), filter)
            .build();
  }

  @Test
  void shouldReturn401WhenGatewayHeadersMissing() throws Exception {
    var result = mvc.perform(get("/api/v1/secure")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(result.getResponse().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  @Test
  void shouldReturn401WhenSharedSecretIsInvalid() throws Exception {
    var result =
        mvc.perform(
                get("/api/v1/secure")
                    .header(ApiKeyAuthFilter.CLIENT_ID_HEADER, UUID.randomUUID().toString())
                    .header(ApiKeyAuthFilter.CLIENT_NAME_HEADER, "Client")
                    .header(ApiKeyAuthFilter.RATE_LIMIT_HEADER, "60")
                    .header(ApiKeyAuthFilter.GATEWAY_AUTH_HEADER, "wrong-secret"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void shouldPassWhenForwardedHeadersAndSharedSecretAreValid() throws Exception {
    var result =
        mvc.perform(
                get("/api/v1/secure")
                    .header(ApiKeyAuthFilter.CLIENT_ID_HEADER, UUID.randomUUID().toString())
                    .header(ApiKeyAuthFilter.CLIENT_NAME_HEADER, "Client")
                    .header(ApiKeyAuthFilter.RATE_LIMIT_HEADER, "60")
                    .header(ApiKeyAuthFilter.GATEWAY_AUTH_HEADER, "shared-secret"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
  }
}
