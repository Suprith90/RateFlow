# Apache Kafka Setup for Notification Service

## Overview

This document explains how Apache Kafka is integrated into the notification service for asynchronous processing of notifications.

## Architecture

```
Notification Creation -> Kafka Producer -> notification-events topic -> Kafka Consumer -> Notification Status Updates
```

## Components

1. **NotificationEvent** - DTO representing events sent to Kafka
2. **NotificationEventProducer** - Publishes events to the `notification-events` topic
3. **NotificationEventConsumer** - Consumes events from the `notification-events` topic and updates notification statuses
4. **MockEmailProvider** & **MockSMSProvider** - Simulate email and SMS sending services

## Topics

- **notification-events**: Main topic for notification processing events
  - Key: Event ID (UUID string)
  - Value: NotificationEvent (JSON serialized)

## Event Types

- `NOTIFICATION_CREATED` - Published when a new notification is created
- `EMAIL_SENT` / `SMS_SENT` - Published when email/SMS is successfully sent
- `EMAIL_FAILED` / `SMS_FAILED` - Published when email/SMS sending fails
- `EMAIL_RETRY` / `SMS_RETRY` - Published when email/SMS needs to be retried

## Local Development Setup

### Prerequisites

- Docker and Docker Compose
- Java 21
- Maven 3.8+

### Running Kafka Locally

The easiest way to run Kafka locally for development is using Docker Compose:

```bash
docker-compose up -d
```

Create a `docker-compose.yml` file with the following content:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.0.1
    hostname: zookeeper
    container_name: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.0.1
    hostname: kafka
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
```

### Configuration

The application uses the following Kafka properties (in `application.properties` or environment variables):

```
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.group-id=notification-processor
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
```

## Implementation Details

### Producer

The `NotificationEventProducer` publishes events when a notification is created:

```java
public void publishNotificationCreated(Notification notification) {
    String eventId = UUID.randomUUID().toString();
    NotificationEvent event = new NotificationEvent(
        eventId,
        notification.getId(),
        "NOTIFICATION_CREATED",
        Instant.now(),
        notification
    );

    kafkaTemplate.send("notification-events", event.getEventId(), event);
}
```

### Consumer

The `NotificationEventConsumer` processes events and updates notification statuses:

```java
@KafkaListener(
    topics = "notification-events",
    groupId = "notification-processor",
    containerFactory = "kafkaListenerContainerFactory"
)
@Transactional
public void consumeNotificationEvent(@Payload NotificationEvent event) {
    // Process event and update notification status
}
```

### Idempotency

The consumer is designed to be idempotent:
- Checks current notification status before processing
- Only processes events that would cause a state transition
- Skips processing if notification is already in a terminal state (SENT or FAILED)

### Error Handling

- Exceptions during event processing are logged
- In production, failed events could be routed to a dead letter queue
- The consumer acknowledges messages only after successful processing

## Testing

### Unit Tests

Unit tests mock Kafka dependencies to test the producer and consumer logic in isolation.

### Integration Tests

Integration tests use Testcontainers to run a real Kafka broker for end-to-end testing:

```java
@Testcontainers
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@EmbeddedKafka(partitions = 1, topics = {"notification-events"})
class KafkaIntegrationTest {
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"));
    
    // Tests...
}
```

## Production Considerations

1. **Topic Configuration**: In production, topics should be created with appropriate partitioning and replication factors
2. **Monitoring**: Monitor consumer lag and processing throughput
3. **Error Handling**: Implement dead letter queues for repeatedly failing messages
4. **Security**: Configure SSL/SASL for production Kafka clusters
5. **Scaling**: Adjust consumer concurrency based on throughput requirements

## Troubleshooting

1. **Consumer Not Receiving Messages**: 
   - Verify consumer group ID matches
   - Check that auto.offset.reset is set appropriately
   - Ensure consumer is subscribed to correct topics

2. **Messages Not Being Produced**:
   - Verify producer is calling send() method
   - Check Kafka broker connectivity
   - Verify serialization configuration

3. **Duplicate Processing**:
   - Ensure idempotency checks are in place
   - Verify consumer group configuration
   - Check for improper commit behavior