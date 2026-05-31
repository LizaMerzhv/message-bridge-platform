# **MessageBridge**

A microservices-based notification platform for creating email notifications, asynchronous RabbitMQ delivery, and delivery status tracking through a public API or Admin UI.

## Highlights

* **Microservice Architecture:** Decoupled Spring Boot services with separate databases.
* **JWT-based Security:** Public API is protected with Bearer JWT tokens issued by Keycloak.
* **Guaranteed Publishing:** Uses the Transactional Outbox pattern to safely publish delivery tasks to RabbitMQ.
* **Resilience & Recovery:** Worker-side retry handling with DLQ routing after exhausted attempts.
* **Admin UI:** Thymeleaf-based interface for viewing notifications and managing templates.

## Tech Stack

* **Core:** Java 21, Spring Boot 3.3.3
* **Spring Ecosystem:** Spring Web, Security, OAuth2 Resource Server, Data JPA, AMQP, Actuator, Spring Cloud Gateway
* **Infrastructure & Storage:** PostgreSQL, Flyway, RabbitMQ, Keycloak, Docker Compose
* **UI:** Thymeleaf
* **Testing:** JUnit 5, Spring Boot Test, Testcontainers

## System Architecture

The ecosystem is composed of 4 application microservices plus local infrastructure:

* `api-gateway` — Single external entry point. Validates JWT tokens, applies in-memory rate limiting and routes API/Admin requests.
* `notification-api-service` — Core API for notifications, templates, idempotency, and outbox publishing.
* `notification-delivery-service` — Worker service that consumes RabbitMQ tasks, sends emails through SMTP/MailHog, applies retry/DLQ logic, and reports delivery results back to the API.
* `admin-service` — Thymeleaf Admin UI protected by HTTP Basic authentication.
* `keycloak` — Identity Provider for issuing JWT tokens.
* `rabbitmq` — Message broker with management UI.
* `mailhog` — Local SMTP server and web UI for inspecting test emails.
* `postgres-api`, `postgres-worker` — Separate PostgreSQL databases for API and worker services.

## Security Model

`/api/**` requires a Bearer JWT, validated by both `api-gateway` and `notification-api-service`.

`/internal/**` is blocked by the gateway and protected with `X-Internal-Auth`.

Admin UI uses HTTP Basic. Default credentials: `admin / admin`.


## Data Flow

1. A client calls `POST /api/v1/notifications` through `api-gateway` with a Bearer JWT.
2. The gateway validates the JWT, applies in-memory rate limiting, and forwards the request to `notification-api-service`.
3. The API validates the JWT again and resolves the client from `azp`, `client_id`, or `sub`.
4. The API validates the payload and applies idempotency by `clientId + externalRequestId`.
5. The notification and outbox record are saved in one database transaction.
6. `OutboxPublisher` reads pending outbox records and publishes delivery tasks to RabbitMQ.
7. `notification-delivery-service` consumes tasks and sends emails through SMTP/MailHog.
8. On success, the worker records the delivery attempt and sends an internal callback to the API.
9. On failure, the worker retries with delay. After the maximum number of attempts, the message is routed to the DLQ and final failure is reported to the API.

## Prerequisites

Before running the project locally, ensure you have:

* Docker and Docker Compose
* Java 21, if running services outside Docker
* Free ports:

```text
8080  — API Gateway
8081  — Keycloak
8090  — Admin UI
8025  — MailHog UI
1025  — MailHog SMTP
5442  — API PostgreSQL
5443  — Worker PostgreSQL
5672  — RabbitMQ
15672 — RabbitMQ Management UI
```

## Quickstart

### 1. Prepare configuration

```bash
cp .env.example .env
```

### 2. Start infrastructure and services

```bash
docker compose up --build -d
```

### 4. Configure Keycloak

Keycloak is available at:

```text
http://localhost:8081
```

Default admin credentials:

```text
username: admin
password: admin
```

The repository does not currently provide automatic realm/client import.

To use the public API, Keycloak must contain a realm matching:

```text
http://localhost:8081/realms/message-bridge
```

The issued JWT must contain a client identity matching the API database value:

```text
demo-client
```

### 5. Open Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

Public API endpoints require a Bearer JWT.

### 6. Create a notification

```bash
curl -i -X POST http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "externalRequestId": "promo-2026-march-001",
    "channel": "EMAIL",
    "to": "customer@example.com",
    "templateCode": "PROMO_DISCOUNT",
    "variables": {
      "customerName": "Alex",
      "discountPercent": "25%",
      "promoCode": "SPRING2026",
      "validUntil": "March 31, 2026",
      "shopUrl": "https://example.com/shop"
    },
    "sendAt": "2026-06-20T09:00:00Z"
  }'
```

### 7. Check delivery status

```bash
curl -i http://localhost:8080/api/v1/notifications/<notification-id> \
  -H "Authorization: Bearer $TOKEN"
```

### 8. Verify email delivery

Open MailHog:

```text
http://localhost:8025
```

### 9. Open Admin UI

Direct access:

```text
http://localhost:8090/admin/ui/notifications
```

Through gateway:

```text
http://localhost:8080/admin/ui/notifications
```

Default credentials:

```text
username: admin
password: admin
```
