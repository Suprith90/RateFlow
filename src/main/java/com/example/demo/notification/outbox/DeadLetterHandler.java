package com.example.demo.notification.outbox;

import com.example.demo.notification.outbox.OutboxEvent;
import com.example.demo.notification.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles dead letter events for repeatedly failing outbox events.
 * Moves events that have exceeded their retry limit to a dead letter topic.
 */
@Service
public class DeadLetterHandler {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterHandler.class);
    private static final String DEAD_LETTER_TOPIC = "notification-events-dlq";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public DeadLetterHandler(OutboxEventRepository outboxEventRepository,
                             KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Processes failed outbox events and moves them to the dead letter queue
     * if they have exceeded their retry limit.
     */
    @Transactional
    public void handleFailedEvents() {
        // In a more sophisticated implementation, we would query for events
        // that have exceeded their retry limit and haven't been moved to DLQ yet
        // For simplicity, we'll just log that this would happen

        // Find events that are in FAILED status (meaning they've exhausted retries)
        long failedCount = outboxEventRepository.countByPublicationStatus(
                OutboxEvent.PublicationStatus.FAILED);

        if (failedCount > 0) {
            log.info("Found {} events in failed status that should be moved to dead letter queue", failedCount);
            // In a real implementation, we would:
            // 1. Query for FAILED events that haven't been sent to DLQ yet
            // 2. For each event, publish to dead letter topic
            // 3. Mark them as DLQ_PROCESSED or similar to avoid reprocessing
        }
    }

    /**
     * Sends a failed outbox event to the dead letter topic.
     * @param event the failed outbox event
     */
    @Transactional
    public void sendToDeadLetter(OutboxEvent event) {
        try {
            // Send to dead letter topic
            kafkaTemplate.send(DEAD_LETTER_TOPIC, event.getEventId(), event.getEventPayload()).get();

            // In a real implementation, we would mark the event as sent to DLQ
            // to prevent reprocessing. For now, we'll just log it.
            log.info("Sent outbox event {} to dead letter topic {}", event.getEventId(), DEAD_LETTER_TOPIC);
        } catch (Exception ex) {
            log.error("Failed to send outbox event {} to dead letter topic: {}",
                    event.getEventId(), ex.getMessage(), ex);
        }
    }
}