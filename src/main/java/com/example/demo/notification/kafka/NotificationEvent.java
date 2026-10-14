package com.example.demo.notification.kafka;

import com.example.demo.notification.Notification;
import java.time.Instant;
import java.util.Objects;

/**
 * Event DTO for Kafka notification processing.
 * Contains only the essential information needed for processing,
 * avoiding unnecessary personal or sensitive data.
 */
public class NotificationEvent {

    private String eventId;
    private Long notificationId;
    private String eventType;
    private Instant createdAt;
    private String notificationType;
    private String recipient;
    private String templateId;
    private String payload;

    // Constructors
    public NotificationEvent() {
        // For frameworks
    }

    public NotificationEvent(String eventId, Long notificationId, String eventType,
                             Instant createdAt, Notification notification) {
        this.eventId = Objects.requireNonNull(eventId);
        this.notificationId = Objects.requireNonNull(notificationId);
        this.eventType = Objects.requireNonNull(eventType);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.notificationType = notification.getType().name();
        this.recipient = notification.getRecipient();
        this.templateId = notification.getTemplateId();
        this.payload = notification.getPayload();
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}