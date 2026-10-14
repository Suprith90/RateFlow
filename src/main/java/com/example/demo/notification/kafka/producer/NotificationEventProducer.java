package com.example.demo.notification.kafka.producer;

import com.example.demo.notification.kafka.NotificationEvent;
import com.example.demo.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka producer for notification events.
 * Publishes events to the notification-events topic for asynchronous processing.
 */
@Service
public class NotificationEventProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventProducer.class);
    private static final String TOPIC = "notification-events";

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public NotificationEventProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish a notification created event.
     * @param notification the notification that was created
     */
    public void publishNotificationCreated(Notification notification) {
        String eventId = UUID.randomUUID().toString();
        NotificationEvent event = new NotificationEvent(
                eventId,
                notification.getId(),
                "NOTIFICATION_CREATED",
                Instant.now(),
                notification
        );

        kafkaTemplate.send(TOPIC, event.getEventId(), event);
        log.debug("Sent notification event {} for notification {} to topic {}",
                eventId, notification.getId(), TOPIC);
    }

    /**
     * Send a notification event to the Kafka topic.
     * @param event the notification event to send
     */
    public void sendNotificationEvent(NotificationEvent event) {
        kafkaTemplate.send(TOPIC, event.getEventId(), event);
        log.debug("Sent notification event {} for notification {} to topic {}",
                event.getEventId(), event.getNotificationId(), TOPIC);
    }
}