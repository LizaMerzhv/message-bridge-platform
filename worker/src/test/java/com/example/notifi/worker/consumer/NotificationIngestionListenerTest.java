package com.example.notifi.worker.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.example.notifi.common.model.Channel;
import com.example.notifi.worker.model.NotificationEntity;
import com.example.notifi.worker.repo.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class NotificationIngestionListenerTest {



    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15.6-alpine")
        .withDatabaseName("notifi_worker")
        .withUsername("notifi_worker")
        .withPassword("notifi_worker")
        .withReuse(true)
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.default-schema", () -> "public");

        registry.add("spring.jpa.hibernate.naming.physical-strategy",
            () -> "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private NotificationRepository notificationRepository;

    private NotificationIngestionListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationIngestionListener(notificationRepository, Clock.systemUTC());
    }

    @Test
    void shouldPersistNotificationFromMessage() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        NotificationTaskMessage message = new NotificationTaskMessage(
            id,
            UUID.randomUUID(),
            "ext",
            Channel.EMAIL,
            "user@example.com",
            "Subject",
            "template",
            null,
            createdAt,
            createdAt,
            0,
            "trace",
            "http://webhook",
            "secret");

        listener.handle(message);

        NotificationEntity persisted = notificationRepository.findById(id).orElseThrow();
        assertThat(persisted.getClientId()).isEqualTo(message.clientId());
        assertThat(persisted.getStatus().name()).isEqualTo("CREATED");
        assertThat(persisted.getWebhookUrl()).isEqualTo("http://webhook");
    }
}
