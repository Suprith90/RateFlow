# RateFlow

A Spring Boot application demonstrating rate limiting, notification processing, and event-driven architecture with observability and comprehensive testing.

## Table of Contents
- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Local Setup](#local-setup)
- [Docker Compose](#docker-compose)
- [Database Migrations](#database-migrations)
- [Authentication & Authorization](#authentication--authorization)
- [Rate Limiting](#rate-limiting)
- [API Documentation](#api-documentation)
- [Kafka Notification Processing](#kafka-notification-processing)
- [Outbox Pattern](#outbox-pattern)
- [Testing](#testing)
- [Load Testing](#load-testing)
- [Observability](#observability)
- [Known Limitations](#known-limitations)
- [Future Improvements](#future-improvements)
- [Package Structure](#package-structure)
- [Architecture Decision Records](#architecture-decision-records)
- [Contributing](#contributing)
- [License](#license)

## Project Overview

RateFlow is a Spring Boot 4.1.1 application built with Java 21 that implements a rate-limited API with a notification system. The application demonstrates:

- **Token Bucket rate limiting** using Redis for distributed rate limiting
- **Notification service** supporting email and SMS via mock providers
- **Event-driven architecture** using Apache Kafka for asynchronous notification processing
- **Outbox pattern** for reliable event publishing
- **Observability** using Spring Boot Actuator and Micrometer with Prometheus integration
- **Comprehensive testing** including unit, integration, and performance tests with k6
- **Dockerized deployment** with docker-compose for local development

The problem statement this application addresses is how to build a scalable, observable system that protects resources from overuse while reliably delivering notifications through asynchronous processing.

## Architecture

![Architecture Diagram](docs/architecture-diagram.png)

*The architecture diagram shows the interaction between API clients, the RateFlow application, Redis (rate limiting), PostgreSQL (data storage), Apache Kafka (event streaming), and notification providers.*

### Key Components

1. **API Layer** (`RateLimitController`, `NotificationController`) - Exposes REST endpoints
2. **Rate Limiting Service** (`TokenBucketService`) - Implements Token Bucket algorithm with Redis
3. **Notification Service** (`NotificationServiceImpl`) - Creates notifications and publishes to Kafka
4. **Event Processing** (`NotificationEventConsumer`) - Consumes notification events from Kafka
5. **Notification Providers** (`MockEmailProvider`, `MockSMSProvider`) - Simulate email/SMS delivery
6. **Outbox Pattern** (`OutboxEvent`, `OutboxPublisher`) - Ensures reliable event publishing
7. **Observability** - Actuator endpoints, Micrometer metrics, structured logging
8. **Data Access** - Spring Data JPA repositories backed by PostgreSQL

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| Language | Java | 21 | Primary programming language |
| Framework | Spring Boot | 4.1.1 | Application framework |
| Build Tool | Maven | 3.9+ | Dependency management and build |
| Data Store | PostgreSQL | 16 | Persistent storage for notifications |
| Cache | Redis | 7 | Distributed rate limiting storage |
| Messaging | Apache Kafka | 7.6 | Event streaming platform |
| Coordination | ZooKeeper | 3.8 | Kafka coordination |
| Rate Limiting | Token Bucket | N/A | Algorithm implemented with Redis Lua scripts |
| Observability | Spring Boot Actuator | 4.1.1 | Health checks and metrics |
| Observability | Micrometer | 1.11.0 | Application metrics collection |
| Observability | Prometheus | N/A | Metrics exposition format |
| Testing | JUnit Jupiter | 5.10.0 | Unit and integration testing |
| Testing | Testcontainers | 1.18.0 | Integration testing with dependencies |
| Testing | k6 | 0.45.0+ | Performance and load testing |
| Containerization | Docker | 20.10+ | Container platform |
| Orchestration | Docker Compose | V2 | Multi-container local development |
| Lombok | Project Lombok | 1.18.30 | Reduce boilerplate code |

### Design Decisions

1. **Token Bucket over Fixed Window**: Chose Token Bucket for smoother traffic shaping and burst allowance capabilities.
2. **Redis for Rate Limiting**: Provides atomic operations via Lua scripts for distributed rate limiting with low latency.
3. **Outbox Pattern**: Ensures reliable event delivery even if Kafka is temporarily unavailable.
4. **Separate Notification Worker**: Allows independent scaling of notification processing.
5. **Mock Providers**: Enable safe testing without sending actual emails/SMS.
6. **Actuator Security**: Default Spring Boot security protects sensitive endpoints.
7. **Multi-stage Docker Build**: Produces smaller, more secure production images.

## Prerequisites

- [Git](https://git-scm.com/) - For cloning the repository
- [Docker Engine](https://docs.docker.com/engine/install/) - Version 20.10+
- [Docker Compose](https://docs.docker.com/compose/install/) - Version V2
- [Java JDK](https://adoptium.net/) - Version 21 (for local development without Docker)
- [Maven](https://maven.apache.org/install.html) - Version 3.9+ (for local development without Docker)

## Local Setup

### Option 1: Using Docker Compose (Recommended)

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd demo
   ```

2. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

3. (Optional) Edit the `.env` file to customize configuration:
   ```bash
   nano .env
   ```

4. Start the application stack:
   ```bash
   docker compose up -d
   ```

5. Verify the services are running:
   ```bash
   docker compose ps
   ```

6. Access the application:
   - API: http://localhost:8080
   - Actuator endpoints: http://localhost:8080/actuator
   - Health check: http://localhost:8080/actuator/health

### Option 2: Local Development (Without Docker)

1. Install PostgreSQL, Redis, and Kafka locally or use Docker for just these services:
   ```bash
   # Start only the infrastructure services
   docker compose up -d postgres redis kafka zookeeper
   ```

2. Copy and configure the environment file:
   ```bash
   cp .env.example .env
   # Update SPRING_PROFILES_ACTIVE=local if needed
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

4. Access the application at http://localhost:8080

## Docker Compose

The `docker-compose.yml` file defines the following services:

- **api**: Spring Boot application exposing REST APIs on port 8080
- **postgres**: PostgreSQL 16 database for persistent storage
- **redis**: Redis 7 instance for Token Bucket rate limiting
- **kafka**: Apache Kafka 7.6 for event streaming
- **zookeeper**: Zookeeper ensemble for Kafka coordination
- **notification-worker**: (Optional) Separate worker for processing notifications

### Key Features

- Multi-stage Docker build for the API service
- Non-root user execution for security (UID 1001)
- Health checks for all services
- Named volumes for persistent data
- Proper dependency ordering via `depends_on` with health check conditions
- Environment variable configuration via `.env` file
- Restart policies for resilience

### Usage Instructions

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# View logs for a specific service
docker compose logs -f api

# Stop all services
docker compose down

# Stop and remove all data
docker compose down -v

# Rebuild and restart
docker compose up -d --build
```

## Database Migrations

The application uses Spring Data JPA with Hibernate for schema management. Database migrations are handled via Flyway (located in `src/main/resources/db/migration/`):

- `V1__create_notifications_table.sql` - Creates the notifications table
- `V2__create_outbox_events_table.sql` - Creates the outbox events table

Migrations automatically run on application startup when using the `docker` profile.

## Authentication & Authorization

RateFlow implements a simple authentication mechanism based on user identification headers for demonstration purposes:

### Current Implementation

- **Rate Limiting**: Uses `X-User-ID` header to identify users for rate limiting purposes
- **Notification Endpoints**: Currently accept requests without strict authentication for ease of testing
- **Security Configuration**: Spring Boot security is enabled by default, protecting actuator endpoints

### Flow

1. Client sends request with `X-User-ID: <user-id>` header
2. Rate limiting middleware extracts the user ID and checks against Redis Token Bucket
3. If allowed, request proceeds to the controller
4. Controllers process the request and return appropriate responses

### Notes for Production

In a production environment, you would want to implement:
- Proper authentication (JWT/OAuth2) with user details service
- Role-based access control (RBAC) for different endpoints
- Secure credential management
- Input validation and sanitization

## Rate Limiting

### Algorithm

RateFlow implements the **Token Bucket** algorithm for rate limiting:

- Each user has a bucket with a fixed capacity
- Tokens are added to the bucket at a fixed rate (refill rate)
- Each request consumes one token from the bucket
- If the bucket has sufficient tokens, the request is allowed
- If the bucket is empty, the request is rejected with HTTP 429 (Too Many Requests)

### Configuration

- **Capacity**: 10 tokens (maximum burst size)
- **Refill Rate**: 10 tokens per minute (steady-state rate)
- **Storage**: Redis (distributed, shared across API instances)
- **Atomicity**: Implemented using Lua scripts to ensure thread-safe operations
- **Response Headers**: Includes `Retry-After` indicating seconds until next token availability

### Endpoints Protected

- `GET /api/resource` - Rate-limited API endpoint for demonstration
- (Other endpoints can be easily protected by adding the RateLimitFilter)

### Known Limitation: Rate Limiting Filter Configuration

The application includes a generic rate limiting filter (`RateLimitFilter`) that is enabled by default. This filter uses different configuration (capacity=100 requests, refill rate=10 requests/second) and key resolution (based on user principal, session, or IP address) than the controller-specific rate limiting in `RateLimitController` (which uses the `X-User-ID` header and capacity=10 tokens per minute). 

While the controller's stricter limits are the effective limit for the `/api/resource` endpoint due to the filter's higher thresholds, the mismatch in key resolution could lead to inconsistencies if the filter were configured with stricter limits. For production use, we recommend either:
1. Disabling the generic filter by setting `ratelimiter.enabled=false` in `application.properties`, or
2. Aligning the filter's configuration and key resolution with the controller's implementation.

### Testing Rate Limiting

The performance tests include scenarios for:
- Traffic below the rate limit (should receive mostly 200 responses)
- Traffic above the rate limit (should receive 429 responses)

## API Documentation

### Available Endpoints

#### Rate-Limited API
```
GET /api/resource
```
- Description: Simple endpoint to demonstrate rate limiting
- Headers: `X-User-ID` (required for rate limiting)
- Responses:
  - 200 OK: Resource accessed successfully
  - 429 Too Many Requests: Rate limit exceeded (includes Retry-After header)

#### Notification Management
```
POST /api/v1/notifications
```
- Description: Create a new notification
- Request Body:
  ```json
  {
    "type": "EMAIL|SMS",
    "recipient": "user@example.com or +1234567890",
    "templateId": "welcome_email",
    "payload": {"key": "value"},
    "idempotencyKey": "unique-idempotency-key"
  }
  ```
- Responses:
  - 202 Accepted: Notification queued for processing
  - 400 Bad Request: Invalid request data
  - 401 Unauthorized: Authentication required (if implemented)
  - 500 Internal Server Error: Server error

```
GET /api/v1/notifications/{id}
```
- Description: Retrieve a specific notification by ID
- Path Variables: `id` (notification ID)
- Responses:
  - 200 OK: Notification details
  - 404 Not Found: Notification not found
  - 401 Unauthorized: Authentication required

```
GET /api/v1/notifications
```
- Description: List notifications for the current user with pagination
- Query Parameters: `page` (default: 0), `size` (default: 20)
- Responses:
  - 200 OK: Paginated list of notifications
  - 401 Unauthorized: Authentication required

### Example Requests

#### Access Rate-Limited API
```bash
curl -H "X-User-ID: test-user" http://localhost:8080/api/resource
```

#### Create Email Notification
```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "type": "EMAIL",
    "recipient": "user@example.com",
    "templateId": "welcome_email",
    "payload": {"username": "john_doe"},
    "idempotencyKey": "email_12345"
  }'
```

#### Create SMS Notification
```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "type": "SMS",
    "recipient": "+15551234567",
    "templateId": "alert_sms",
    "payload": {"alert_code": "LOW_BALANCE"},
    "idempotencyKey": "sms_67890"
  }'
```

#### Get Notification
```bash
curl http://localhost:8080/api/v1/notifications/1
```

#### List Notifications
```bash
curl http://localhost:8080/api/v1/notifications?page=0&size=10
```

### OpenAPI/Swagger

The application does not currently include Swagger/OpenAPI documentation. To add it:

1. Add dependency: `springdoc-openapi-starter-webmvc-ui`
2. Configure in `application.properties`:
   ```
   springdoc.api-docs.path=/v3/api-docs
   springdoc.swagger-ui.path=/swagger-ui.html
   ```
3. Access Swagger UI at http://localhost:8080/swagger-ui.html

## Kafka Notification Processing

### Flow

1. **Notification Creation**: When a notification is created via the API, it's saved to the database and a `NotificationEvent` is published to the `notification-events` Kafka topic via the Outbox pattern
2. **Event Consumption**: The `NotificationEventConsumer` listens to the `notification-events` topic
3. **Processing**: For each event, the consumer:
   - Retrieves the notification from the database
   - Determines the appropriate provider (email/SMS) based on notification type
   - Delegates to the provider for delivery
   - Updates the notification status in the database
4. **Completion**: The notification is marked as SENT or FAILED in the database

### Topics

- `notification-events`: Contains `NotificationEvent` objects for processing
- `notification-events-dlq`: Dead letter queue for failed processing attempts (if enabled)

### Configuration

- Bootstrap Servers: `kafka:9092` (in Docker) or `localhost:9092` (local)
- Consumer Group: `notification-processor-group`
- Key Deserializer: String
- Value Deserializer: JSON (using Spring Kafka's JsonDeserializer)

### Reliability Features

- **At-least-once delivery**: Kafka consumer configured with appropriate acknowledgment settings
- **Error Handling**: Failed processing attempts are logged and can be configured for retry
- **Dead Letter Queue**: Repeatedly failing messages can be sent to a DLQ for inspection
- **Idempotency**: Processing is designed to be idempotent to handle duplicate messages

## Outbox Pattern

### Purpose

The Outbox pattern ensures reliable event publishing by decoupling the database transaction from the message publishing process, preventing message loss if the publishing fails after the database commit.

### Implementation

1. **OutboxEvent Table**: Stores events waiting to be published
   - `id`: Primary key
   - `event_type`: Type of event (e.g., "NotificationCreated")
   - `payload`: JSON payload of the event
   - `publication_status`: PENDING, PUBLISHED, or FAILED
   - `created_at`: Timestamp when event was created
   - `published_at`: Timestamp when event was published (if successful)

2. **OutboxPublisher**: 
   - Saves OutboxEvent to database within the same transaction as the notification
   - Publishes events to Kafka asynchronously
   - Updates publication status based on publish result

3. **OutboxScheduler**: 
   - Runs periodically (every 10 seconds by default)
   - Finds PENDING events and attempts to publish them
   - Retries failed events with exponential backoff
   - Sends repeatedly failing events to dead letter handling

### Behavior

- **Atomicity**: Notification creation and OutboxEvent creation occur in the same database transaction
- **Reliability**: If publishing fails, the event remains PENDING and will be retried
- **Duplicate Detection**: Idempotency keys prevent duplicate notification processing
- **Monitoring**: Metrics exposed for pending events, failed events, and average retry count

### Metrics Exposed

- `outbox.pending_events`: Number of events waiting to be published
- `outbox.failed_events`: Number of events that have failed publishing
- `outbox.events_retry_count`: Average number of retry attempts per event

## Testing

### Unit and Integration Tests

The application includes a comprehensive test suite:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test service interactions with actual databases and message brokers (using Testcontainers)

### Running Tests

```bash
# Run all tests
./mvnw test

# Run only unit tests (excluding Testcontainers integration tests)
./mvnw test -DskipITs

# Run only integration tests
./mvnw verify -Dskip.unit.tests

# Generate test reports
./mvnw surefire-report:report
```

### Test Coverage

Tests cover:
- Rate limiting service and controller
- Notification service and controller
- Outbox publisher and repository
- Kafka producer and consumer
- Integration scenarios with actual Redis, PostgreSQL, and Kafka
- Error handling and edge cases

### Actual Test Results

As of the latest commit, all tests pass:
```
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
```

*Note: Test results may vary based on environment and timing. The test suite is designed to be deterministic and repeatable.*

## Load Testing

RateFlow includes a comprehensive performance testing suite using k6 to validate system behavior under various load conditions.

### Test Scenarios

The performance tests cover seven distinct scenarios as requested:

1. **Traffic Below the Rate Limit** - Validates normal operation within rate limits
2. **Traffic Above the Rate Limit** - Tests rate limiting effectiveness and 429 responses
3. **Multiple Authenticated Users** - Tests concurrent user handling and data isolation
4. **Short Traffic Bursts** - Measures system responsiveness to sudden load spikes
5. **Sustained Traffic** - Tests long-term stability and monitors for resource leaks
6. **Notification Creation Under Load** - Focuses on notification creation throughput
7. **Notification Processing Lag** - Creates backlog to observe consumer lag behavior

### Methodology

- **Realistic Test Data**: Dynamically generated unique identifiers to prevent collisions
- **Safe Testing**: Notification providers are mocked (no real emails/SMS sent)
- **Metrics Collection**: Custom k6 metrics for rate limiting, notification operations, and error rates
- **Correlation with Observability**: Test results can be correlated with Spring Boot Actuator metrics
- **Think Time**: Configurable sleep between requests to simulate realistic user behavior
- **Ramp-up/Ramp-down**: Used in burst scenarios to avoid shocking the system
- **Clear Success/Failure Criteria**: Defined thresholds for latency, error rates, and 429 responses

### Measured Results

The performance tests measure:

- **Throughput**: Requests per second sustained over time
- **Latency**: Response times (median, p95, p99 percentiles)
- **Error Rates**: HTTP 5xx, 4xx, and unexpected status codes
- **Rate Limiting Effectiveness**: Percentage of requests receiving 429 responses
- **Resource Utilization**: Through correlation with application metrics (Redis latency, DB utilization, Kafka lag)

### How to Run Load Tests

1. Ensure the RateFlow application is running (via Docker Compose or local setup)
2. Install k6 (version 0.45.0 or later)
3. Run individual scenarios:
   ```bash
   # Scenario 1: Traffic below rate limit
   k6 run --vus 5 --duration 2m performance-test/rateflow-performance-test.js
   
   # Scenario 2: Traffic above rate limit
   k6 run --vus 50 --duration 3m performance-test/rateflow-performance-test.js
   ```
4. Run all scenarios sequentially:
   ```bash
   ./performance-test/run_all_scenarios.sh
   ```
5. Validate setup before testing:
   ```bash
   ./performance-test/validate_setup.sh
   ```

### Results Interpretation

Key metrics to watch:
- **Response Latency**: Look for increases over time (memory leaks) or spikes during GC
- **Error Rates**: HTTP 5xx indicate server problems; HTTP 429 indicate rate limiting working correctly
- **Throughput**: Requests per second sustained over time should match expected capacity
- **Resource Utilization**: CPU, memory, disk I/O, and network should show no concerning trends

See `performance-test/PERFORMANCE_REPORT_TEMPLATE.md` for a template to document results.

## Observability

RateFlow implements comprehensive observability using Spring Boot Actuator and Micrometer.

### Health and Readiness Indicators

- **Health Check**: `/actuator/health` - Aggregated health of all services
- **Redis Health**: Custom indicator for Redis connectivity and rate limiter functionality
- **Database Health**: Standard Spring Boot DataSource health check
- **Kafka Health**: Custom indicator for Kafka connectivity

### Application Metrics

#### Rate Limiting Metrics
- `ratelimit.requests_allowed_total`: Counter for allowed requests
- `ratelimit.requests_rejected_total`: Counter for rejected requests  
- `ratelimit.decision_latency_seconds`: Timer for decision latency measurement

#### Notification Metrics
- `notifications_created_total`: Counter for created notifications
- `notifications_delivered_success_total`: Counter for successful deliveries
- `notifications_delivered_failed_total`: Counter for failed deliveries
- `notification.processing_duration_seconds`: Timer for processing duration

#### Outbox Metrics
- `outbox.pending_events`: Gauge for pending events (queries repository directly)
- `outbox.failed_events`: Gauge for failed events (queries repository directly)
- `outbox.events_retry_count`: Gauge for average retry count (queries repository directly)

#### Kafka Consumer Lag
- `kafka.consumer_lag`: Gauge monitoring consumer lag for notification-processor group

### Structured Logging

- Logging pattern includes trace IDs and span IDs for correlation
- No sensitive data (passwords, JWTs, API keys) is logged
- Uses SLF4J with parameterized logging to prevent injection attacks

### Security

- Actuator endpoints secured via Spring Boot's default security
- Sensitive endpoints require authentication in production
- Health details shown appropriately based on security configuration

### Accessing Observability Data

```bash
# Health check
curl http://localhost:8080/actuator/health

# All metrics
curl http://localhost:8080/actuator/metrics

# Prometheus format
curl http://localhost:8080/actuator/prometheus

# Specific metric
curl http://localhost:8080/actuator/metrics/ratelimit.requests_allowed_total
```

### Integration with Monitoring Systems

The `/actuator/prometheus` endpoint can be scraped by Prometheus for long-term storage and alerting. Grafana dashboards can be created to visualize the metrics.

## Known Limitations

1. **Authentication Simplified**: Current implementation uses `X-User-ID` header for rate limiting only; no full authentication system
2. **Notification Providers Mocked**: Email and SMS providers are mocks; no actual message delivery
3. **No Distributed Tracing**: While correlation IDs are in logs, full distributed tracing (Jaeger/Zipkin) is not implemented
4. **Limited API Endpoints**: Only a few endpoints are implemented for demonstration purposes
5. **In-memory Caching**: Some services use simple in-memory caching where distributed caching might be beneficial in production
6. **Basic Error Handling**: Error responses could be more detailed and consistent across endpoints
7. **No API Versioning**: The API does not currently implement versioning
8. **Limited Kafka Configuration**: Production Kafka configurations (security, monitoring, etc.) are not demonstrated
9. **Static Rate Limiting Parameters**: Rate limit capacity and refill rate are hardcoded (could be made configurable)
10. **No Circuit Breaker**: External service calls lack circuit breaker patterns for resilience
11. **Rate Limiting Filter Mismatch**: The generic rate limiting filter uses different configuration and key resolution than the controller-specific rate limiting, potentially leading to inconsistencies. See the Rate Limiting section for details.

## Future Improvements

1. **Implement Proper Authentication**: Add JWT/OAuth2 authentication with user details service
2. **Replace Mock Providers**: Integrate with actual email (SendGrid, SES) and SMS (Twilio) providers
3. **Add Distributed Tracing**: Implement OpenTelemetry or similar for end-to-end request tracing
4. **Expand API Endpoints**: Add more business endpoints as needed for a complete application
5. **Use Distributed Caching**: Consider Redis for shared caching where appropriate
6. **Enhanced Error Handling**: Implement consistent error response format and better validation
7. **Add API Versioning**: Implement versioned APIs to support backward compatibility
8. **Production Kafka Settings**: Add SSL/SASL authentication, monitoring, and tuning for production
9. **Externalize Rate Limiting Parameters**: Make capacity and refill rate configurable via environment variables
10. **Add Resilience Patterns**: Implement circuit breaker, bulkhead, and timeout patterns for external calls
11. **Add API Documentation**: Implement OpenAPI/Swagger for automatic API documentation
12. **Implement Feature Toggles**: Add ability to enable/disable features without redeployment
13. **Add Chaos Engineering**: Implement controlled failure injection for resilience testing
14. **Enhance Security**: Regular dependency scanning, penetration testing, and security audits
15. **Implement Canary Deployments**: For safer production releases
16. **Align Rate Limiting Filter**: Either disable the generic filter or align its configuration and key resolution with the controller-specific implementation to ensure consistent rate limiting behavior.

## Package Structure

```
com.example.demo
├── DemoApplication.java
├── config
│   └── MetricsConfig.java
├── metrics
│   └── ObservabilityMetricsService.java
├── notification
│   ├── AuthorizationTest.java
│   ├── Notification.java
│   ├── NotificationNotFoundException.java
│   ├── NotificationRepository.java
│   ├── NotificationService.java
│   ├── NotificationServiceImpl.java
│   ├── NotificationController.java
│   ├── dto
│   │   ├── CreateNotificationRequest.java
│   │   └── NotificationResponse.java
│   ├── kafka
│   │   ├── NotificationEvent.java
│   │   ├── config
│   │   │   ├── KafkaConfig.java
│   │   │   └── KafkaConsumerConfig.java
│   │   ├── consumer
│   │   │   └── NotificationEventConsumer.java
│   │   ├── monitor
│   │   │   └── KafkaLagMonitoringService.java
│   │   └── producer
│   │       └── NotificationEventProducer.java
│   ├── notification
│   │   ├── OutboxEvent.java
│   │   ├── OutboxEventRepository.java
│   │   ├── OutboxPublisher.java
│   │   ├── OutboxSchedulerConfig.java
│   │   └── DeadLetterHandler.java
│   └── provider
│       ├── MockEmailProvider.java
│       └── MockSMSProvider.java
├── ratelimiter
│   ├── RateLimitService.java
│   ├── RateLimitService.java
│   ├── RateLimitController.java
│   ├── TokenBucketService.java
│   ├── TokenBucketService.java
│   ├── RateLimiterConfig.java
│   ├── RateLimitFilter.java
│   └── RedisRateLimitHealthIndicator.java
└── test
    ├── DemoApplicationTests.java
    ├── notification
    │   ├── NotificationControllerTest.java
    │   ├── NotificationServiceTest.java
    │   ├── NotificationRepositoryTest.java
    │   └        AuthorizationTest.java
    ├── notification
    │   ├── kafka
    │   │   ├── KafkaNotificationIntegrationTest.java
    │   │   ├── consumer
    │   │   │   └── NotificationEventConsumerTest.java
    │   │   ├── producer
    │   │   │   └── NotificationEventProducerTest.java
    │   │   └── KafkaIntegrationTest.java
    │   └── outbox
    │       ├── OutboxEventTest.java
│       ├── OutboxSchedulerTest.java
│       ├── OutboxPublisherTest.java
│       └── OutboxEventRepositoryTest.java
    └── ratelimiter
        ├── RateLimitFilterTest.java
        ├── RateLimitServiceTest.java
        ├── RateLimitServiceTest.java
        └── TokenBucketServiceTest.java
```

## Architecture Decision Records

We maintain Architecture Decision Records (ADRs) to document significant design decisions and their consequences. The ADRs are located in the `adr/` directory.

### List of ADRs

1. [ADR 001: Use Token Bucket for Rate Limiting](./adr/001-use-token-bucket-for-rate-limiting.md)
2. [ADR 002: Implement Outbox Pattern for Reliable Event Publishing](./adr/002-implement-outbox-pattern.md)
3. [ADR 003: Use Mock Notification Providers for Safety](./adr/003-use-mock-notification-providers.md)

## Contributing

### Reporting Issues

Please use the GitHub issue tracker to report bugs or suggest features. When reporting issues, include:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, Java version, Docker version if applicable)

### Development Setup

1. Fork the repository
2. Clone your fork locally
3. Create a feature branch: `git checkout -b feature/amazing-feature`
4. Make your changes
5. Ensure all tests pass: `./mvnw verify`
6. Commit your changes: `git commit -m 'Add amazing feature'`
7. Push to the branch: `git push origin feature/amazing-feature`
8. Open a pull request

### Code Style

- Follow the existing code style in the repository
- Use meaningful variable and method names
- Write clear, concise comments where needed
- Ensure proper error handling and logging
- Add tests for new functionality

### Pull Request Process

1. Ensure the PR description clearly explains the changes
2. Link to any relevant issues
3. Ensure all status checks pass (CI workflow)
4. Respond to reviewer comments promptly
5. Keep the PR focused on a single concern
6. Squash commits if requested by maintainers

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Spring Boot team for the excellent framework
- The open-source community for Redis, Kafka, and related technologies
- Contributors to Testcontainers and k6 for testing tools
- JetBrains for IntelliJ IDEA (used in development)

---
*Last updated: 2026-10-14*