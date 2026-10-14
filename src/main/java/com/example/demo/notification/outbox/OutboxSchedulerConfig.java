package com.example.demo.notification.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for publishing outbox events.
 * Runs periodically to process pending outbox events.
 */
@Component
@EnableScheduling
class OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);
    private final OutboxPublisher outboxPublisher;

    OutboxScheduler(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * Publishes pending outbox events every 5 seconds.
     * Adjust the frequency based on your throughput requirements.
     */
    @Scheduled(fixedDelayString = "${outbox.publish.interval.ms:5000}")
    void publishOutboxEvents() {
        try {
            outboxPublisher.publishPendingEvents();
        } catch (Exception ex) {
            log.error("Error in outbox publisher: {}", ex.getMessage(), ex);
        }
    }
}