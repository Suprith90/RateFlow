package com.example.demo.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Notification entity representing a message to be sent to a user.
 * <p>
 * This entity stores the notification metadata and status, but not the actual
 * delivery mechanism which is handled by external services.
 */
@Entity
@Table(name = "notifications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "idempotency_key"}))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who owns this notification (derived from authenticated principal) */
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    /** Type of notification: EMAIL or SMS */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** Recipient of the notification (email address or phone number) */
    @Column(nullable = false)
    private String recipient;

    /** Template identifier for the notification content */
    @Column(nullable = false)
    private String templateId;

    /** Payload data for template substitution (JSON format) */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /** Current status of the notification */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    /** Number of retry attempts made */
    private int retryCount = 0;

    /** Maximum number of retry attempts allowed */
    private int maxRetryCount = 3;

    /** Backoff multiplier for retry delays (exponential backoff base) */
    private int retryBackoffMultiplier = 2;

    /** Timestamp when notification was created */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Timestamp when notification was last updated */
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    /** Idempotency key to prevent duplicate submissions */
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    /** Timestamp when notification was sent (or attempted) */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** Timestamp when notification was completed (success or failure) */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Error message if notification failed */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Constructors
    protected Notification() {
        // For JPA
    }

    public Notification(String userId, NotificationType type, String recipient,
                        String templateId, String payload, String idempotencyKey) {
        this.userId = Objects.requireNonNull(userId);
        this.type = Objects.requireNonNull(type);
        this.recipient = Objects.requireNonNull(recipient);
        this.templateId = Objects.requireNonNull(templateId);
        this.payload = Objects.requireNonNull(payload);
        this.idempotencyKey = idempotencyKey;
        this.status = NotificationStatus.QUEUED;
    }

    /**
     * Constructor for testing - allows setting the ID explicitly.
     */
    public Notification(Long id, String userId, NotificationType type, String recipient,
                        String templateId, String payload, String idempotencyKey) {
        this.userId = Objects.requireNonNull(userId);
        this.type = Objects.requireNonNull(type);
        this.recipient = Objects.requireNonNull(recipient);
        this.templateId = Objects.requireNonNull(templateId);
        this.payload = Objects.requireNonNull(payload);
        this.idempotencyKey = idempotencyKey;
        this.id = id;
        this.status = NotificationStatus.QUEUED;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getRecipient() { return recipient; }
    public String getTemplateId() { return templateId; }
    public String getPayload() { return payload; }
    public NotificationStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetryCount() { return maxRetryCount; }
    public int getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }

    public void setStatus(NotificationStatus status) {
        this.status = Objects.requireNonNull(status);
        this.updatedAt = Instant.now();
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        this.updatedAt = Instant.now();
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        this.updatedAt = Instant.now();
    }

    public void setRetryBackoffMultiplier(int retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
        this.updatedAt = Instant.now();
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
        this.updatedAt = Instant.now();
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        this.updatedAt = Instant.now();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public void setPayload(String payload) {
        this.payload = payload;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the notification as sent.
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the notification as failed.
     * @param errorMessage description of the failure
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the notification as ready for retry.
     */
    public void markAsRetryPending() {
        this.status = NotificationStatus.RETRY_PENDING;
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the notification can be retried based on retry count and max retry count.
     * @return true if the notification can be retried, false otherwise
     */
    public boolean canRetry() {
        return retryCount < maxRetryCount;
    }

    /**
     * Calculates the retry delay based on exponential backoff.
     * @param baseDelayMillis base delay in milliseconds
     * @return delay in milliseconds for this retry attempt
     */
    public long calculateRetryDelay(long baseDelayMillis) {
        return baseDelayMillis * (long) Math.pow(retryBackoffMultiplier, retryCount);
    }

    // Enums
    public enum NotificationType {
        EMAIL,
        SMS
    }

    public enum NotificationStatus {
        QUEUED,        // Waiting to be processed
        PROCESSING,    // Currently being processed by external service
        SENT,          // Successfully sent to external service
        FAILED,        // Failed to send (no retries left)
        RETRY_PENDING  // Failed but will be retried
    }
}