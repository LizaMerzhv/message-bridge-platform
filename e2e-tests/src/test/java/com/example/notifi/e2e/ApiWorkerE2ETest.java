package com.example.notifi.e2e;

import com.example.notifi.api.NotifiApiApplication;
import com.example.notifi.api.core.notification.NotificationView;
import com.example.notifi.api.data.entity.NotificationStatus;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationRequest;
import com.example.notifi.api.web.shared.notification.dto.CreateNotificationResponse;
import com.example.notifi.api.model.Channel;
import com.example.notifi.worker.NotifiWorkerApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
class ApiWorkerE2ETest {

    private static final Logger log = LoggerFactory.getLogger(ApiWorkerE2ETest.class);

    @Container
    private static final PostgreSQLContainer<?> apiDb =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notifi_api")
            .withUsername("notifi_api")
            .withPassword("notifi_api");

    @Container
    private static final PostgreSQLContainer<?> workerDb =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notifi_worker")
            .withUsername("notifi_worker")
            .withPassword("notifi_worker");

    @Container
    private static final GenericContainer<?> rabbit =
        new GenericContainer<>("rabbitmq:3.13.3").withExposedPorts(5672);

    @Container
    private static final GenericContainer<?> mailhog =
        new GenericContainer<>("mailhog/mailhog:v1.0.1").withExposedPorts(1025, 8025);

    private static ConfigurableApplicationContext apiContext;
    private static ConfigurableApplicationContext workerContext;
    private static RestClient apiClient;
    private static RestClient mailhogClient;

    @BeforeAll
    static void startEnvironment() {
        Startables.deepStart(java.util.stream.Stream.of(apiDb, workerDb, rabbit, mailhog)).join();

        configureApiDatasource();
        apiContext = new SpringApplicationBuilder(NotifiApiApplication.class)
            .web(WebApplicationType.SERVLET)
            .properties(apiProperties())
            .run();

        int apiPort = ((ServletWebServerApplicationContext) apiContext).getWebServer().getPort();
        apiClient = RestClient.builder()
            .baseUrl("http://localhost:" + apiPort)
            .defaultHeaders(headers -> headers.set("X-API-Key", "demo-123"))
            .build();

        configureWorkerDatasource();
        workerContext = new SpringApplicationBuilder(NotifiWorkerApplication.class)
            .web(WebApplicationType.SERVLET)
            .properties(workerProperties(apiPort))
            .run();

        mailhogClient = RestClient.builder()
            .baseUrl("http://%s:%d".formatted(mailhog.getHost(), mailhog.getMappedPort(8025)))
            .build();

        logContainers();
    }

    @AfterAll
    static void shutdown() {
        if (workerContext != null) {
            workerContext.close();
        }
        if (apiContext != null) {
            apiContext.close();
        }
        mailhog.stop();
        rabbit.stop();
        apiDb.stop();
        workerDb.stop();
    }

    @AfterEach
    void resetMailhog() {
        purgeMailhog();
    }

    @Test
    void pipelineDeliversEmailAndUpdatesStatus() {
        String subject = "E2E pipeline";
        UUID notificationId = createNotification("e2e-" + UUID.randomUUID(), subject).getId();

        NotificationView delivered = Awaitility.await()
            .atMost(Duration.ofSeconds(45))
            .pollInterval(Duration.ofMillis(500))
            .until(
                () -> fetchNotification(notificationId),
                view -> view.getStatus() == NotificationStatus.SENT);

        assertThat(delivered.getDeliveries())
            .withFailMessage("Expected at least one delivery record after status update")
            .isNotEmpty();

        assertThat(fetchMailhogSubject())
            .as("Mailhog should capture email matching request subject")
            .isEqualTo(subject);
    }

    private NotificationView fetchNotification(UUID id) {
        return apiClient.get()
            .uri("/api/v1/notifications/{id}", id)
            .retrieve()
            .body(NotificationView.class);
    }

    private CreateNotificationResponse createNotification(String externalRequestId, String subject) {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId(externalRequestId);
        request.setChannel(Channel.EMAIL);
        request.setTo("recipient@example.com");
        request.setSubject(subject);

        return apiClient.post()
            .uri("/api/v1/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(CreateNotificationResponse.class);
    }

    private String fetchMailhogSubject() {
        JsonNode result = mailhogClient.get()
            .uri("/api/v2/messages")
            .retrieve()
            .body(JsonNode.class);

        JsonNode items = result.path("items");
        if (items.isArray() && items.size() > 0) {
            JsonNode headers = items.get(0).path("Content").path("Headers");
            JsonNode subjectNode = headers.path("Subject");
            if (subjectNode.isArray() && subjectNode.size() > 0) {
                return subjectNode.get(0).asText();
            }
        }
        return null;
    }

    private void purgeMailhog() {
        try {
            mailhogClient.delete().uri("/api/v1/messages").retrieve();
        } catch (Exception ignored) {
            // ignore purge failures
        }
    }

    private static void configureApiDatasource() {
        System.setProperty("spring.datasource.url", apiDb.getJdbcUrl());
        System.setProperty("spring.datasource.username", apiDb.getUsername());
        System.setProperty("spring.datasource.password", apiDb.getPassword());
    }

    private static void configureWorkerDatasource() {
        System.setProperty("spring.datasource.url", workerDb.getJdbcUrl());
        System.setProperty("spring.datasource.username", workerDb.getUsername());
        System.setProperty("spring.datasource.password", workerDb.getPassword());
    }

    private static Map<String, Object> apiProperties() {
        return Map.of(
            "server.port", 0,
            "spring.datasource.url", apiDb.getJdbcUrl(),
            "spring.datasource.username", apiDb.getUsername(),
            "spring.datasource.password", apiDb.getPassword(),
            "spring.rabbitmq.host", rabbit.getHost(),
            "spring.rabbitmq.port", rabbit.getMappedPort(5672),
            "notifi.amqp.exchange", "notifi.e2e.exchange",
            "notifi.amqp.ingest-routing-key", "ingest",
            "notifi.outbox.poll-interval-ms", 500,
            "logging.level.com.example.notifi", "INFO"
        );
    }

    private static Map<String, Object> workerProperties(int apiPort) {
        return Map.ofEntries(
            Map.entry("server.port", 0),
            Map.entry("spring.datasource.url", workerDb.getJdbcUrl()),
            Map.entry("spring.datasource.username", workerDb.getUsername()),
            Map.entry("spring.datasource.password", workerDb.getPassword()),
            Map.entry("spring.rabbitmq.host", rabbit.getHost()),
            Map.entry("spring.rabbitmq.port", rabbit.getMappedPort(5672)),
            Map.entry("API_INTERNAL_BASE_URL", "http://localhost:" + apiPort),
            Map.entry("notifi.amqp.exchange", "notifi.e2e.exchange"),
            Map.entry("notifi.amqp.ingest-routing-key", "ingest"),
            Map.entry("notifi.scheduler.scan-interval-ms", 500),
            Map.entry("notifi.retry.max-attempts", 0),
            Map.entry("smtp.host", mailhog.getHost()),
            Map.entry("smtp.port", mailhog.getMappedPort(1025)),
            Map.entry("logging.level.com.example.notifi", "INFO")
        );
    }

    private static void logContainers() {
        Slf4jLogConsumer consumer = new Slf4jLogConsumer(log);
        rabbit.followOutput(consumer);
        mailhog.followOutput(consumer);
    }
}
