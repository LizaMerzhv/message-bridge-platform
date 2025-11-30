package com.example.notifi.api.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class FlywaySchemaTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15.6-alpine")
          .withDatabaseName("notifi_api")
          .withUsername("notifi_api")
          .withPassword("notifi_api")
          .withReuse(true)
          .waitingFor(Wait.forListeningPort());

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired JdbcTemplate jdbc;

  @Test
  void shouldCreateRequiredTables() {
    assertThat(reg("public.client")).isNotNull();
    assertThat(reg("public.template")).isNotNull();
    assertThat(reg("public.notification")).isNotNull();
    assertThat(reg("public.delivery")).isNotNull();
  }

  @Test
  void shouldCreateExpectedIndexes() {
    assertThat(reg("public.ix_notification_status_send_at")).isNotNull();
    assertThat(reg("public.ix_notification_client_created_at")).isNotNull();
    assertThat(reg("public.ix_delivery_notification_attempt")).isNotNull();
    // assertThat(reg("public.ix_delivery_status")).isNotNull();
  }

  @Test
  void shouldEnforceXorConstraintOnNotification() {
    assertThatThrownBy(
            () ->
                jdbc.execute(
                    """
                INSERT INTO notification
                  (id,"clientId","externalRequestId",channel,"to","sendAt",status,attempts,"createdAt","updatedAt")
                VALUES
                  (gen_random_uuid(), gen_random_uuid(), 'dup', 'email', 'u@example.com', now(), 'CREATED', 0, now(), now())
            """))
        .hasRootCauseInstanceOf(SQLException.class)
        .hasMessageContaining("notification_subject_template_xor");
  }

  private String reg(String name) {
    return jdbc.queryForObject("SELECT to_regclass(?)", String.class, name);
  }
}
