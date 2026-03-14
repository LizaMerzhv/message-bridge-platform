package com.example.notifi.api.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> commonTagsCustomizer() {
    return registry -> registry.config().commonTags("application", "notifi-api");
  }
}
