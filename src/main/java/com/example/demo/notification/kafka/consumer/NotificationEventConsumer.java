package com.example.demo.notification.kafka.consumer;

import com.example.demo.notification.Notification;
import com.example.demo.notification.NotificationRepository;
import com.example.demo.notification.Notification.NotificationStatus;
import com.example.demo.notification.kafka.NotificationEvent;
import com.example.demo.notification.outbox.OutboxEvent;
import com.example.demo.notification.outbox.OutboxEventRepository;
import com.example.demo.notification.provider.MockEmailProvider;
import com.example.demo.notification.provider.MockSMSProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer for notification events.
 * Processes events from the notification-events topic and updates notification statuses.
 * Uses the outbox pattern for reliable event publishing.
 */
@Service
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);
    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MockEmailProvider emailProvider;
    private final MockSMSProvider smsProvider;

    public NotificationEventConsumer(NotificationRepository notificationRepository,
                                     OutboxEventRepository outboxEventRepository,
                                     MockEmailProvider emailProvider,
                                     MockSMSProvider smsProvider) {
        this.notificationRepository = notificationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.emailProvider = emailProvider;
        this.smsProvider = smsProvider;
    }

    /**
     * Listens to the notification-events topic and processes notification events.
     * Instead of directly publishing events, saves them to the outbox table for reliable publishing.
     * @param event the notification event to process
     */
    @KafkaListener(
        topics = "notification-events",
        groupId = "notification-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeNotificationEvent(@Payload NotificationEvent event) {
        log.info("Received notification event {} for notification {}",
                event.getEventId(), event.getNotificationId());

        try {
            // Find the notification by ID
            Notification notification = notificationRepository.findById(event.getNotificationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found with ID: " + event.getNotificationId()));

            // Update notification status based on event type
            switch (event.getEventType()) {
                case "NOTIFICATION_CREATED":
                    // When a notification is created, set status to QUEUED for processing
                    // and attempt to send it via the appropriate provider
                    // Only process if notification is newly created (status is null)
                    if (notification.getStatus() == null) {
                        notification.setStatus(NotificationStatus.QUEUED);
                        notificationRepository.save(notification);
                        log.info("Set notification {} status to QUEUED", notification.getId());

                        // Attempt to send the notification based on its type
                        // Instead of directly calling providers, create outbox events for their results
                        switch (notification.getType()) {
                            case EMAIL:
                                // Create outbox event for email sending attempt
                                createEmailSendingOutboxEvent(notification);
                                break;
                            case SMS:
                                // Create outbox event for SMS sending attempt
                                createSmsSendingOutboxEvent(notification);
                                break;
                        }
                    } else {
                        log.info("Notification {} already has status {}, skipping processing",
                                notification.getId(), notification.getStatus());
                    }
                    break;

                case "EMAIL_SENT":
                case "SMS_SENT":
                    // When delivery is successful, set status to SENT
                    // Only update if not already in a terminal state
                    if (notification.getStatus() != NotificationStatus.SENT &&
                            notification.getStatus() != NotificationStatus.FAILED) {
                        notification.setStatus(NotificationStatus.SENT);
                        notificationRepository.save(notification);
                        log.info("Set notification {} status to SENT", notification.getId());
                    } else {
                        log.info("Notification {} already in final state {}, skipping",
                                notification.getId(), notification.getStatus());
                    }
                    break;

                case "EMAIL_FAILED":
                case "SMS_FAILED":
                    // When delivery fails, set status to FAILED
                    // Only update if not already in a terminal state
                    if (notification.getStatus() != NotificationStatus.SENT &&
                            notification.getStatus() != NotificationStatus.FAILED) {
                        notification.setStatus(NotificationStatus.FAILED);
                        notificationRepository.save(notification);
                        log.warn("Set notification {} status to FAILED", notification.getId());
                    } else {
                        log.info("Notification {} already in final state {}, skipping",
                                notification.getId(), notification.getStatus());
                    }
                    break;

                case "EMAIL_RETRY":
                case "SMS_RETRY":
                    // When delivery needs retry, set status to RETRY_PENDING
                    // and attempt to send it again with retry logic
                    // Only process if not already in a terminal state
                    if (notification.getStatus() != NotificationStatus.SENT &&
                            notification.getStatus() != NotificationStatus.FAILED) {
                        notification.setStatus(NotificationStatus.RETRY_PENDING);
                        notificationRepository.save(notification);
                        log.info("Set notification {} status to RETRY_PENDING", notification.getId());

                        // Attempt to send the notification again based on its type
                        switch (notification.getType()) {
                            case EMAIL:
                                // Create outbox event for email retry attempt
                                createEmailRetryOutboxEvent(notification);
                                break;
                            case SMS:
                                // Create outbox event for SMS retry attempt
                                createSmsRetryOutboxEvent(notification);
                                break;
                        }
                    } else {
                        log.info("Notification {} already in final state {}, skipping retry",
                                notification.getId(), notification.getStatus());
                    }
                    break;

                default:
                    log.warn("Unknown event type: {}", event.getEventType());
                    break;
            }

        } catch (Exception e) {
            log.error("Error processing notification event {}: {}",
                    event.getEventId(), e.getMessage(), e);
            // In a production system, we might want to send this to a dead letter queue
            // or implement retry logic here
        }
    }

    /**
     * Creates an outbox event for an email sending attempt.
     * @param notification the notification to send
     */
    private void createEmailSendingOutboxEvent(Notification notification) {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent outboxEvent = new OutboxEvent(
                eventId,
                notification.getId(),
                "EMAIL_SENT_ATTEMPT",
                /* In a real implementation, we would serialize relevant data here */
                "{\"notificationId\": \"" + notification.getId() + "\", \"recipient\": \"" + notification.getRecipient() + "\"}"
        );
        outboxEventRepository.save(outboxEvent);
        log.debug("Created outbox event {} for email sending attempt on notification {}", eventId, notification.getId());
    }

    /**
     * Creates an outbox event for an SMS sending attempt.
     * @param notification the notification to send
     */
    private void createSmsSendingOutboxEvent(Notification notification) {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent outboxEvent = new OutboxEvent(
                eventId,
                notification.getId(),
                "SMS_SENT_ATTEMPT",
                /* In a real implementation, we would serialize relevant data here */
                "{\"notificationId\": \"" + notification.getId() + "\", \"recipient\": \"" + notification.getRecipient() + "\"}"
        );
        outboxEventRepository.save(outboxEvent);
        log.debug("Created outbox event {} for SMS sending attempt on notification {}", eventId, notification.getId());
    }

    /**
     * Creates an outbox event for an email retry attempt.
     * @param notification the notification to send
     */
    private void createEmailRetryOutboxEvent(Notification notification) {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent outboxEvent = new OutboxEvent(
                eventId,
                notification.getId(),
                "EMAIL_RETRY_ATTEMPT",
                /* In a real implementation, we would serialize relevant data here */
                "{\"notificationId\": \"" + notification.getId() + "\", \"recipient\": \"" + notification.getRecipient() + "\"}"
        );
        outboxEventRepository.save(outboxEvent);
        log.debug("Created outbox event {} for email retry attempt on notification {}", eventId, notification.getId());
    }

    /**
     * Creates an outbox event for an SMS retry attempt.
     * @param notification the notification to send
     */
    private void createSmsRetryOutboxEvent(Notification notification) {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent outboxEvent = new OutboxEvent(
                eventId,
                notification.getId(),
                "SMS_RETRY_ATTEMPT",
                /* In a real implementation, we would serialize relevant data here */
                "{\"notificationId\": \"" + notification.getId() + "\", \"recipient\": \"" + notification.getRecipient() + "\"}"
        );
        outboxEventRepository.save(outboxEvent);
        log.debug("Created outbox event {} for SMS retry attempt on notification {}", eventId, notification.getId());
    }
}