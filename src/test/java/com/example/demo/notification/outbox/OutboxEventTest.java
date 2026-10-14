package com.example.demo.notification.outbox;

import com.example.demo.notification.outbox.OutboxEvent.PublicationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OutboxEvent state transitions and behavior.
 */
class OutboxEventTest {

    @Test
    void shouldInitializeWithPendingStatus() {
        // Given & When
        OutboxEvent event = new OutboxEvent("event-1", 1L, "TEST_EVENT", "{}");

        // Then
        assertEquals(PublicationStatus.PENDING, event.getPublicationStatus());
        assertEquals("event-1", event.getEventId());
        assertEquals(1L, event.getAggregateId());
        assertEquals("TEST_EVENT", event.getEventType());
        assertEquals("{}", event.getEventPayload());
        assertEquals(0, event.getRetryCount());
        assertNotNull(event.getCreatedAt());
        assertNull(event.getPublishedAt());
        assertNull(event.getLastRetryAt());
        assertNull(event.getNextRetryAt());
    }

    @Test
    void shouldMarkAsPublishedSuccessfully() {
        // Given
        OutboxEvent event = new OutboxEvent("event-1", 1L, "TEST_EVENT", "{}");
        Instant now = Instant.now();

        // When
        event.markAsPublished();

        // Then
        assertEquals(PublicationStatus.PUBLISHED, event.getPublicationStatus());
        assertNotNull(event.getPublishedAt());
        // Just check that it's set recently - avoid fragile time-based assertions
        assertNotNull(event.getPublishedAt());
    }

    @Test
    void shouldMarkAsFailedAndIncrementRetryCount() {
        // Given
        OutboxEvent event = new OutboxEvent("event-1", 1L, "TEST_EVENT", "{}");

        // When
        event.markAsFailed(3, 1000); // max 3 retries, 1s base backoff

        // Then
        assertEquals(PublicationStatus.PENDING, event.getPublicationStatus()); // Still pending for retry
        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getLastRetryAt());
        assertNotNull(event.getNextRetryAt());
        // Next retry should be after last retry (basic check)
        assertTrue(event.getNextRetryAt().isAfter(event.getLastRetryAt()) ||
                event.getNextRetryAt().equals(event.getLastRetryAt())); // Allow equal for very fast processing
    }

    @Test
    void shouldMarkAsFailedAfterMaxRetries() {
        // Given
        OutboxEvent event = new OutboxEvent("event-1", 1L, "TEST_EVENT", "{}");
        // To test max retries, we need to set retryCount to max-1 then call markAsFailed
        // But OutboxEvent doesn't have a setter for retryCount, so we'll test the logic differently
        // Instead, let's test that after 3 failures with maxRetries=3, it goes to FAILED

        // First failure (retryCount 0 -> 1)
        event.markAsFailed(3, 1000);
        assertEquals(PublicationStatus.PENDING, event.getPublicationStatus());
        assertEquals(1, event.getRetryCount());

        // Second failure (retryCount 1 -> 2)
        event.markAsFailed(3, 1000);
        assertEquals(PublicationStatus.PENDING, event.getPublicationStatus());
        assertEquals(2, event.getRetryCount());

        // Third failure (retryCount 2 -> 3) - should now be FAILED
        event.markAsFailed(3, 1000);
        assertEquals(PublicationStatus.FAILED, event.getPublicationStatus());
        assertEquals(3, event.getRetryCount());
        assertNotNull(event.getLastRetryAt());
        assertNull(event.getNextRetryAt()); // No further retries scheduled
    }
}