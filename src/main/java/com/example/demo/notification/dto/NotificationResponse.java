package com.example.demo.notification.dto;

import com.example.demo.notification.Notification;
import java.time.Instant;
import java.io.Serializable;

/**
 * Response DTO for notification data.
 * Contains the information to be returned to clients about a notification.
 */
public class NotificationResponse implements Serializable {

    private Long id;
    private String userId;
    private String type;
    private String recipient;
    private String templateId;
    private String payload;
    private String status;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;
    private String idempotencyKey;
    private Instant sentAt;
    private Instant completedAt;
    private String errorMessage;

    // Constructors
    public NotificationResponse() {
        // For frameworks
    }

    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.userId = notification.getUserId();
        this.type = notification.getType().name();
        this.recipient = notification.getRecipient();
        this.templateId = notification.getTemplateId();
        this.payload = notification.getPayload();
        this.status = notification.getStatus().name();
        this.retryCount = notification.getRetryCount();
        this.createdAt = notification.getCreatedAt();
        this.updatedAt = notification.getUpdatedAt();
        this.idempotencyKey = notification.getIdempotencyKey();
        this.sentAt = notification.getSentAt();
        this.completedAt = notification.getCompletedAt();
        this.errorMessage = notification.getErrorMessage();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}