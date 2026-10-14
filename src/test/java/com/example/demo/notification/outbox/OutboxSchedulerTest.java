package com.example.demo.notification.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OutboxScheduler.
 */
@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private OutboxPublisher outboxPublisher;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @Test
    void shouldDelegateToOutboxPublisherWhenPublishingEvents() throws Exception {
        // When
        outboxScheduler.publishOutboxEvents();

        // Then
        verify(outboxPublisher).publishPendingEvents();
    }

    @Test
    void shouldHandlePublisherFailureGracefully() throws Exception {
        // Given
        doThrow(new RuntimeException("Publisher failure")).when(outboxPublisher).publishPendingEvents();

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> outboxScheduler.publishOutboxEvents());

        // Verify the attempt was made
        verify(outboxPublisher).publishPendingEvents();
    }
}