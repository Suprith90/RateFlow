package com.example.demo.notification.outbox;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Outbox event for reliable event publishing.
 * Stores events that need to be published to external systems like Kafka.
 */
@Entity
@Table(name = "outbox_events",
       indexes = {
           @Index(name = "idx_outbox_events_publication_status", columnList = "publication_status"),
           @Index(name = "idx_outbox_events_next_retry_at", columnList = "next_retry_at"),
           @Index(name = "idx_outbox_events_aggregate_id", columnList = "aggregate_id")
       },
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id"}))
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique event identifier for deduplication */
    @Column(nullable = false, updatable = false)
    private String eventId;

    /** ID of the aggregate/notification this event belongs to */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /** Type of the event (e.g., NOTIFICATION_CREATED, EMAIL_SENT) */
    @Column(nullable = false)
    private String eventType;

    /** Serialized event payload (JSON) */
    @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
    private String eventPayload;

    /** Publication status: PENDING, PUBLISHED, FAILED */
    @Column(name = "publication_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PublicationStatus publicationStatus = PublicationStatus.PENDING;

    /** Timestamp when event was created */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Timestamp when event was successfully published */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Number of retry attempts made */
    @Column(name = "retry_count")
    private int retryCount = 0;

    /** Timestamp of last retry attempt */
    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    /** Timestamp when next retry should be attempted */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    // Constructors
    protected OutboxEvent() {
        // For JPA
    }

    public OutboxEvent(String eventId, Long aggregateId, String eventType, String eventPayload) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventPayload = eventPayload;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getEventPayload() { return eventPayload; }
    public PublicationStatus getPublicationStatus() { return publicationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getRetryCount() { return retryCount; }
    public Instant getLastRetryAt() { return lastRetryAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }

    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEventPayload(String eventPayload) { this.eventPayload = eventPayload; }
    public void setPublicationStatus(PublicationStatus publicationStatus) { this.publicationStatus = publicationStatus; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public void setLastRetryAt(Instant lastRetryAt) { this.lastRetryAt = lastRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    /**
     * Marks the event as published successfully.
     */
    public void markAsPublished() {
        this.publicationStatus = PublicationStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    /**
     * Marks the event as failed and schedules a retry if applicable.
     * @param maxRetries maximum number of retry attempts allowed
     * @param backoffMillis base backoff time in milliseconds
     */
    public void markAsFailed(int maxRetries, long backoffMillis) {
        this.retryCount++;
        this.lastRetryAt = Instant.now();

        if (this.retryCount >= maxRetries) {
            this.publicationStatus = PublicationStatus.FAILED;
        } else {
            this.publicationStatus = PublicationStatus.PENDING;
            // Exponential backoff: backoffMillis * (2 ^ retryCount)
            long delay = backoffMillis * (long) Math.pow(2, this.retryCount - 1);
            this.nextRetryAt = Instant.now().plusSeconds(delay / 1000);
        }
    }

    /**
     * Publication status of an outbox event.
     */
    public enum PublicationStatus {
        PENDING,    // Waiting to be published
        PUBLISHED,  // Successfully published
        FAILED      // Failed after max retries
    }
}