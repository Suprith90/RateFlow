package com.example.demo.notification.provider;

import com.example.demo.notification.Notification;
import com.example.demo.notification.kafka.NotificationEvent;
import com.example.demo.notification.kafka.producer.NotificationEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Mock email provider for simulating email sending.
 * In a real application, this would integrate with an actual email service like SendGrid, SES, etc.
 */
@Service
public class MockEmailProvider {

    private static final Logger log = LoggerFactory.getLogger(MockEmailProvider.class);
    private final NotificationEventProducer eventProducer;

    public MockEmailProvider(NotificationEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    /**
     * Sends an email notification.
     * Simulates random success/failure for demonstration purposes.
     * @param notification the notification to send
     */
    public void sendEmail(Notification notification) {
        log.info("Attempting to send email notification {} to {}",
                notification.getId(), notification.getRecipient());

        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate random success/failure (70% success rate)
        boolean success = Math.random() < 0.7;

        if (success) {
            log.info("Successfully sent email notification {} to {}",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "EMAIL_SENT",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        } else {
            log.warn("Failed to send email notification {} to {}: Simulated failure",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "EMAIL_FAILED",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        }
    }

    /**
     * Sends an email notification with retry logic.
     * @param notification the notification to send
     */
    public void sendEmailWithRetry(Notification notification) {
        log.info("Attempting to send email notification {} to {} (retry)",
                notification.getId(), notification.getRecipient());

        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate random success/failure (80% success rate on retry)
        boolean success = Math.random() < 0.8;

        if (success) {
            log.info("Successfully sent email notification {} to {} on retry",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "EMAIL_SENT",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        } else {
            log.warn("Failed to send email notification {} to {} on retry: Will retry again later",
                    notification.getId(), notification.getRecipient());
            NotificationEvent event = new NotificationEvent(
                    java.util.UUID.randomUUID().toString(),
                    notification.getId(),
                    "EMAIL_RETRY",
                    Instant.now(),
                    notification
            );
            eventProducer.sendNotificationEvent(event);
        }
    }
}