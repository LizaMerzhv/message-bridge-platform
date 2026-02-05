package com.example.notifi.api.data;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.notifi.api.data.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class NotificationConstraintsTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15.6-alpine")
          .withDatabaseName("notifi")
          .withUsername("notifi")
          .withPassword("notifi")
          .withReuse(true);

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private EntityManager em;

  private UUID clientId;

  @BeforeEach
  void setUp() {
    clientId = UUID.randomUUID();

    ClientEntity client = new ClientEntity();
    client.setId(clientId);
    client.setName("Client");
    client.setApiKey("api-key");
    client.setRateLimitPerMin(60);
    client.setCreatedAt(Instant.now());
    client.setUpdatedAt(Instant.now());
    em.persist(client);

    TemplateEntity template = new TemplateEntity();
    template.setId(UUID.randomUUID());
    template.setCode("WELCOME");
    template.setSubject("Welcome");
    template.setBodyHtml("<p>Hi</p>");
    template.setBodyText("Hi");
    template.setStatus(TemplateStatus.ACTIVE);
    template.setCreatedAt(Instant.now());
    template.setUpdatedAt(Instant.now());
    em.persist(template);

    em.flush();
    em.clear();
  }

  @Test
  void shouldEnforceXorConstraint() {
    NotificationEntity entity = baseNotification();
    entity.setSubject("Subject");
    entity.setTemplateCode("WELCOME");
    assertConstraintViolation(() -> persist(entity), "notification_subject_template_xor");
  }

  @Test
  void shouldEnforceUniqueClientExternal() {
    NotificationEntity first = baseNotification();
    persist(first);

    NotificationEntity duplicate = baseNotification();
    duplicate.setExternalRequestId(first.getExternalRequestId());
    assertConstraintViolation(() -> persist(duplicate), "uq_notification_client_external");
  }

  @Test
  void shouldEnforceSubjectLength() {
    NotificationEntity entity = baseNotification();
    entity.setSubject("a".repeat(201)); // > 200
    assertConstraintViolation(() -> persist(entity), "ck_notification_subject_len");
  }

  // ---------- helpers ----------

  private NotificationEntity baseNotification() {
    NotificationEntity e = new NotificationEntity();
    e.setId(UUID.randomUUID());
    e.setClientId(clientId);
    e.setExternalRequestId("ext-" + UUID.randomUUID());
    e.setChannel("email");
    e.setTo("user@example.com");
    e.setStatus(NotificationStatus.CREATED);
    e.setAttempts(0);
    e.setSendAt(Instant.now());
    e.setCreatedAt(Instant.now());
    e.setUpdatedAt(Instant.now());
    e.setSubject("Subject");
    e.setVariables(Map.of("k", "v"));
    return e;
  }

  private void persist(NotificationEntity entity) {
    em.persist(entity);
    em.flush();
    em.clear();
  }

  private void assertConstraintViolation(ThrowingCallable action, String expectedConstraint) {
    assertThatThrownBy(action)
        .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class)
        .hasMessageContaining(expectedConstraint);
  }
}
