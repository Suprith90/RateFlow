# ADR 002: Implement Outbox Pattern for Reliable Event Publishing

## Status
Accepted

## Date
2026-10-14

## Context
We need to ensure that notification events are reliably published to Kafka even if there are temporary failures in the messaging system or network issues. A direct approach of publishing to Kafka after committing the database transaction risks losing events if the publish fails after the commit.

## Decision
We implemented the Outbox pattern to guarantee reliable event publishing by decoupling the database transaction from the message publishing process.

## Consequences

### Pros
- **Reliability Guarantee**: Ensures that every notification creation results in an event being published to Kafka, even if Kafka is temporarily unavailable
- **Atomicity**: Notification creation and event recording happen in the same database transaction, preventing inconsistencies
- **Retry Mechanism**: Built-in retry mechanism with exponential backoff for handling transient failures
- **Visibility**: Events are stored in a database table, allowing for inspection and manual replay if needed
- **Decoupling**: The notification service is not blocked waiting for Kafka to be available

### Cons
- **Increased Complexity**: Additional components (OutboxEvent table, OutboxPublisher, OutboxScheduler) increase system complexity
- **Storage Overhead**: Requires additional database table to store outbox events
- **Polling Overhead**: OutboxScheduler uses polling to check for pending events (mitigated by reasonable polling interval)
- **Eventual Consistency**: There is a delay between notification creation and event publication (typically seconds)

### Implementation Details
- **OutboxEvent Table**: 
  - `id`: Primary key
  - `event_type`: Type of event (e.g., "NotificationCreated")
  - `payload`: JSON payload containing event data
  - `publication_status`: PENDING, PUBLISHED, or FAILED
  - `created_at`: Timestamp when event was created
  - `published_at`: Timestamp when event was published (if successful)
- **OutboxPublisher**: 
  - Saves OutboxEvent to database within the same transaction as the notification
  - Publishes events to Kafka asynchronously via NotificationEventProducer
  - Updates publication status based on publish result
- **OutboxScheduler**: 
  - Runs periodically (every 10 seconds by default)
  - Finds PENDING events and attempts to publish them
  - Retries failed events with configurable backoff
  - Sends repeatedly failing events to dead letter handling after max retries

## Related Decisions
- ADR 001: Use Token Bucket for Rate Limiting
- ADR 003: Use Mock Notification Providers for Safety

## Notes
The Outbox pattern implementation is located in:
- `src/main/java/com/example/demo/notification/outbox/OutboxEvent.java`
- `src/main/java/com/example/demo/notification/outbox/OutboxEventRepository.java`
- `src/main/java/com/example/demo/notification/outbox/OutboxPublisher.java`
- `src/main/java/com/example/demo/notification/outbox/OutboxSchedulerConfig.java`
- Database migration: `src/main/resources/db/migration/V2__create_outbox_events_table.sql`