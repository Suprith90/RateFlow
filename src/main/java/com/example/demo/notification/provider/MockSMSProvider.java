package com.example.demo.notification.provider;

import com.example.demo.notification.Notification;
import com.example.demo.notification.kafka.NotificationEvent;
import com.example.demo.notification.kafka.producer.NotificationEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Mock SMS provider for simulating SMS sending.
 * In a real application, this would integrate with an actual SMS service like Twilio, Nexmo, etc.
 */
@Service
public class MockSMSProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSMSProvider.class);
    private final NotificationEventProducer eventProducer;

    public MockSMSProvider(NotificationEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    /**
     * Sends an SMS notification.
     * Simulates random success/failure for demonstration purposes.
     * @param notification the notification to send
     */
    public void sendSMS(Notification notification) {
        log.info("Attempting to send SMS notification {} to {}",
                notification.getId(), notification.getRecipient());

        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate random success/failure (75% success rate)
        boolean success = Math.random() < 0.75;

        if (success) {
            log.info("Successfully sent SMS notification {} to {}",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "SMS_SENT",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        } else {
            log.warn("Failed to send SMS notification {} to {}: Simulated failure",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "SMS_FAILED",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        }
    }

    /**
     * Sends an SMS notification with retry logic.
     * @param notification the notification to send
     */
    public void sendSMSWithRetry(Notification notification) {
        log.info("Attempting to send SMS notification {} to {} (retry)",
                notification.getId(), notification.getRecipient());

        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate random success/failure (85% success rate on retry)
        boolean success = Math.random() < 0.85;

        if (success) {
            log.info("Successfully sent SMS notification {} to {} on retry",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "SMS_SENT",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        } else {
            log.warn("Failed to send SMS notification {} to {} on retry: Will retry again later",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "SMS_RETRY",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        }
    }
}