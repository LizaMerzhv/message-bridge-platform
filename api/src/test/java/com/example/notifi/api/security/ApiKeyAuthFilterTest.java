package com.example.notifi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.notifi.common.security.ResolvedClientPrincipal;
import com.example.notifi.api.test.WebTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiKeyAuthFilterTest extends WebTestBase {

  private ApiKeyResolverClient apiKeyResolverClient;
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
    this.apiKeyResolverClient = Mockito.mock(ApiKeyResolverClient.class);
    this.filter =
        new ApiKeyAuthFilter(apiKeyResolverClient, new ObjectMapper().findAndRegisterModules());
    this.filter.init(new MockFilterConfig());

    this.mvc =
        MockMvcBuilders.standaloneSetup(new DummyController())
            .addFilters(new RequestIdFilter(), filter)
            .build();
  }

  @Test
  void should_return_401_when_missing_key() throws Exception {
    var result = mvc.perform(get("/api/v1/secure")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(result.getResponse().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
  }

  @Test
  void should_pass_when_valid_key() throws Exception {
    var resolvedPrincipal =
        new ResolvedClientPrincipal(UUID.randomUUID(), "Client", 60);

    when(apiKeyResolverClient.resolveByApiKey(anyString())).thenReturn(Optional.of(resolvedPrincipal));

    var result =
        mvc.perform(get("/api/v1/secure").header(ApiKeyAuthFilter.HEADER, "key")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
  }
}
