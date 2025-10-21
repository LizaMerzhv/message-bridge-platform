/*package com.example.notifi.it;

import com.example.notifi.api.NotifiApiApplication;
import com.example.notifi.api.config.AmqpProperties;
import com.example.notifi.api.security.ApiKeyAuthFilter;
import com.example.notifi.api.security.RequestIdFilter;
import com.example.notifi.common.dto.CreateNotificationRequest;
import com.example.notifi.common.dto.CreateNotificationResponse;
import com.example.notifi.common.model.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
@Testcontainers
@SpringBootTest(classes = NotifiApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiPostgresRabbitIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15.6-alpine")
                    .withDatabaseName("notifi")
                    .withUsername("notifi")
                    .withPassword("notifi")
                    .withReuse(true);
    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private DirectExchange exchange;
    @Autowired
    private AmqpProperties amqpProperties;
    private RabbitAdmin rabbitAdmin;
    private Queue queue;
    private static final String API_KEY = "integration-key";
    @BeforeEach
    void init() {
        rabbitAdmin = new RabbitAdmin(rabbitTemplate);
        queue = new Queue(amqpProperties.getRouting().getTasks(), true);
        rabbitAdmin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue)
                .to(exchange)
                .with(amqpProperties.getRouting().getTasks());
        rabbitAdmin.declareBinding(binding);
        rabbitAdmin.purgeQueue(queue.getName(), true);
        jdbcTemplate.update("DELETE FROM notification");
        jdbcTemplate.update("DELETE FROM client");
        jdbcTemplate.update(
                "INSERT INTO client (id, name, \"apiKey\", \"rateLimitPerMin\", \"createdAt\", \"updatedAt\") VALUES (?,?,?,?,?,?)",
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                "Integration Client",
                API_KEY,
                100,
                Instant.now(),
                Instant.now());
    }
    @Test
    void shouldCreateAndPublishImmediately() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("int-1");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Hello");
        ResponseEntity<CreateNotificationResponse> response = postNotification(request);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        CreateNotificationResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        Map<String, Object> message = receiveMessage();
        assertThat(message).isNotNull();
        assertThat(message.get("notificationId")).isEqualTo(body.getId().toString());
        assertThat(message.get("traceId")).isNotNull();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE id = ?", Integer.class, body.getId());
        assertThat(count).isEqualTo(1);
    }
    @Test
    void shouldCreateWithoutPublishingWhenDelayed() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("int-2");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Hello");
        request.setSendAt(Instant.now().plusSeconds(600));
        ResponseEntity<CreateNotificationResponse> response = postNotification(request);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        CreateNotificationResponse body = response.getBody();
        assertThat(body).isNotNull();
        Awaitility.await().atMost(Duration.ofSeconds(2)).until(() -> messageCount() == 0);
    }
    @Test
    void shouldReturnReplayedOnDuplicateRequest() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setExternalRequestId("int-3");
        request.setChannel(Channel.EMAIL);
        request.setTo("user@example.com");
        request.setSubject("Hello");
        ResponseEntity<CreateNotificationResponse> first = postNotification(request);
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        ResponseEntity<CreateNotificationResponse> second = postNotification(request);
        assertThat(second.getStatusCode().value()).isEqualTo(200);
        assertThat(second.getHeaders().getFirst("X-Idempotency-Replayed")).isEqualTo("true");
    }
    private ResponseEntity<CreateNotificationResponse> postNotification(CreateNotificationRequest request)
            throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(ApiKeyAuthFilter.HEADER, API_KEY);
        headers.set(RequestIdFilter.HEADER_NAME, "it-trace");
        String body = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity("/api/v1/notifications", entity, CreateNotificationResponse.class);
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> receiveMessage() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Object payload = rabbitTemplate.receiveAndConvert(queue.getName());
            assertThat(payload).isInstanceOf(Map.class);
            payloadRef.set((Map<String, Object>) payload);
        });
        return payloadRef.get();
    }
    private int messageCount() {
        Properties properties = rabbitAdmin.getQueueProperties(queue.getName());
        if (properties == null) {
            return 0;
        }
        Object count = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        return count instanceof Number ? ((Number) count).intValue() : 0;
    }
}*/
