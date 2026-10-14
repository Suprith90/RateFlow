package com.example.demo.notification.outbox;

import com.example.demo.metrics.ObservabilityMetricsService;
import com.example.demo.notification.kafka.producer.NotificationEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Publishes outbox events to Kafka reliably.
 * Reads pending events from the outbox table and publishes them,
 * marking them as published only after successful Kafka acknowledgement.
 */
@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 10;
    private static final long BACKOFF_MILLIS = 1000; // 1 second base backoff
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationEventProducer notificationEventProducer;
    private final KafkaTemplate<String, String> kafkaTemplate; // Simplified for String payload
    private final ObservabilityMetricsService metricsService;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           NotificationEventProducer notificationEventProducer,
                           KafkaTemplate<String, String> kafkaTemplate,
                           ObservabilityMetricsService metricsService) {
        this.outboxEventRepository = outboxEventRepository;
        this.notificationEventProducer = notificationEventProducer;
        this.kafkaTemplate = kafkaTemplate;
        this.metricsService = metricsService;
    }

    /**
     * Publishes pending outbox events to Kafka.
     * This method should be called periodically (e.g., by a scheduler).
     */
    @Transactional
    public void publishPendingEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEventsReadyForProcessing(now, BATCH_SIZE);

        if (pendingEvents.isEmpty()) {
            log.debug("No pending outbox events to publish");
            return;
        }

        log.info("Publishing {} outbox events to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            publishEvent(event);
        }
    }

    /**
     * Publishes a single outbox event to Kafka.
     * Uses optimistic locking to prevent concurrent publishers from processing the same event.
     * @param event the outbox event to publish
     */
    @Transactional
    public void publishEvent(OutboxEvent event) {
        // Refresh the event to get the latest state (prevents lost updates)
        OutboxEvent currentEvent = outboxEventRepository.findById(event.getId())
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + event.getId()));

        // Only process if still pending and ready for retry
        if (currentEvent.getPublicationStatus() != OutboxEvent.PublicationStatus.PENDING ||
                (currentEvent.getNextRetryAt() != null && currentEvent.getNextRetryAt().isAfter(Instant.now()))) {
            return; // Skip if not ready or already being processed by another instance
        }

        log.info("Publishing outbox event {} (attempt {})", currentEvent.getEventId(), currentEvent.getRetryCount() + 1);

        // Measure the processing duration
        Instant startTime = Instant.now();

        try {
            // Publish to Kafka synchronously for simplicity
            // In production, you might want to use async with callbacks
            kafkaTemplate.send("notification-events", currentEvent.getEventId(), currentEvent.getEventPayload()).get();

            markAsPublished(currentEvent);

            // Record successful delivery
            metricsService.recordNotificationDeliveredSuccess();

            long durationMillis = java.time.Duration.between(startTime, Instant.now()).toMillis();
            metricsService.recordNotificationProcessingDuration(durationMillis / 1000.0);

            log.info("Successfully published outbox event {} to Kafka", currentEvent.getEventId());
        } catch (InterruptedException | ExecutionException ex) {
            markAsFailed(currentEvent, ex);

            // Record failed delivery
            metricsService.recordNotificationDeliveredFailed();

            long durationMillis = java.time.Duration.between(startTime, Instant.now()).toMillis();
            metricsService.recordNotificationProcessingDuration(durationMillis / 1000.0);

            log.warn("Failed to publish outbox event {} to Kafka: {}",
                    currentEvent.getEventId(), ex.getMessage(), ex);
            // Restore interrupted state
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            markAsFailed(currentEvent, ex);

            // Record failed delivery
            metricsService.recordNotificationDeliveredFailed();

            long durationMillis = java.time.Duration.between(startTime, Instant.now()).toMillis();
            metricsService.recordNotificationProcessingDuration(durationMillis / 1000.0);

            log.error("Error publishing outbox event {}: {}",
                    currentEvent.getEventId(), ex.getMessage(), ex);
        }
    }

    /**
     * Marks an outbox event as published successfully.
     * @param event the outbox event to mark as published
     */
    @Transactional
    public void markAsPublished(OutboxEvent event) {
        event.markAsPublished();
        outboxEventRepository.save(event);
    }

    /**
     * Marks an outbox event as failed and updates retry metadata.
     * @param event the outbox event that failed
     * @param exception the exception that caused the failure
     */
    @Transactional
    public void markAsFailed(OutboxEvent event, Exception exception) {
        event.markAsFailed(MAX_RETRIES, BACKOFF_MILLIS);
        outboxEventRepository.save(event);
    }
}