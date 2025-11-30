package com.example.notifi.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.notifi.api.test.WebTestBase;
import com.example.notifi.api.web.error.ProblemDetails;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RateLimitInterceptorTest extends WebTestBase {

  private RateLimiter rateLimiter;
  private Clock clock;
  private MockMvc mvc;

  @RestController
  static class DummyController {
    @GetMapping("/ping")
    public String ping() {
      return "ok";
    }
  }

  @BeforeEach
  void setUpMvc() {
    this.rateLimiter = Mockito.mock(RateLimiter.class);
    this.clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiter, clock, om);

    this.mvc =
        MockMvcBuilders.standaloneSetup(new DummyController()).addInterceptors(interceptor).build();
  }

  @Test
  void should_pass_when_allowed() throws Exception {
    when(rateLimiter.checkAndConsume(any(), anyInt(), any()))
        .thenReturn(new RateLimitDecision(true, 5, 0));

    var principal =
        new ClientPrincipal(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "Test", 10);

    var result =
        mvc.perform(get("/ping").requestAttr(ClientPrincipal.class.getName(), principal))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getHeader("X-RateLimit-Limit")).isEqualTo("10");
    assertThat(result.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("5");
    Mockito.verify(rateLimiter, times(1)).checkAndConsume(any(), anyInt(), any());
  }

  @Test
  void should_block_with_429_problem_when_exceeded() throws Exception {
    when(rateLimiter.checkAndConsume(any(), anyInt(), any()))
        .thenReturn(new RateLimitDecision(false, 0, 17));

    var principal =
        new ClientPrincipal(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Test", 10);

    var result =
        mvc.perform(get("/ping").requestAttr(ClientPrincipal.class.getName(), principal))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(result.getResponse().getHeader("X-RateLimit-Limit")).isEqualTo("10");
    assertThat(result.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    assertThat(result.getResponse().getHeader("Retry-After")).isEqualTo("17");
    assertThat(result.getResponse().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    ProblemDetails body =
        om.readValue(result.getResponse().getContentAsByteArray(), ProblemDetails.class);
    assertThat(body.getStatus()).isEqualTo(429);
    assertThat(body.getTitle()).isEqualTo("Too Many Requests");
    assertThat(body.getDetail().toLowerCase()).contains("rate limit");
    assertThat(body.getTraceId()).isEqualTo("test-trace");
  }
}
