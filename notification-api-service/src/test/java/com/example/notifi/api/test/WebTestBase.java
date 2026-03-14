package com.example.notifi.api.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.MDC;

public abstract class WebTestBase {
  protected final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void putTrace() {
    MDC.put("traceId", "test-trace");
  }

  @AfterEach
  void clearTrace() {
    MDC.clear();
  }
}
