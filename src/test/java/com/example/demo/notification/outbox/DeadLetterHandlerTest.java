package com.example.demo.notification.outbox;

import com.example.demo.notification.outbox.DeadLetterHandler;
import com.example.demo.notification.outbox.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeadLetterHandler.
 */
@ExtendWith(MockitoExtension.class)
class DeadLetterHandlerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private DeadLetterHandler deadLetterHandler;

    private OutboxEvent failedEvent;

    @BeforeEach
    void setUp() {
        failedEvent = new OutboxEvent("failed-event-1", 1L, "TEST_EVENT", "{\"test\": \"data\"}");
        failedEvent.setRetryCount(5); // Exceeds max retries
        failedEvent.setPublicationStatus(OutboxEvent.PublicationStatus.FAILED);
    }

    @Test
    void shouldSendFailedEventToDeadLetterTopic() {
        // Given
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(eq("notification-events-dlq"), eq("failed-event-1"), eq("{\"test\": \"data\"}")))
                .thenReturn(future);

        // When
        deadLetterHandler.sendToDeadLetter(failedEvent);

        // Then
        verify(kafkaTemplate).send(
                eq("notification-events-dlq"),
                eq("failed-event-1"),
                eq("{\"test\": \"data\"}")
        );
    }

    @Test
    void shouldHandleSendFailureGracefully() {
        // Given
        when(kafkaTemplate.send(
                eq("notification-events-dlq"),
                eq("failed-event-1"),
                eq("{\"test\": \"data\"}")
        )).thenThrow(new RuntimeException("Kafka failure"));

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> deadLetterHandler.sendToDeadLetter(failedEvent));

        // Verify the attempt was made
        verify(kafkaTemplate).send(
                eq("notification-events-dlq"),
                eq("failed-event-1"),
                eq("{\"test\": \"data\"}")
        );
    }

    @Test
    void shouldHandleFailedEvents_findAndProcessFailedEvents() {
        // Given
        when(outboxEventRepository.countByPublicationStatus(
                eq(OutboxEvent.PublicationStatus.FAILED)
        )).thenReturn(3L);

        // When
        deadLetterHandler.handleFailedEvents();

        // Then
        verify(outboxEventRepository).countByPublicationStatus(
                eq(OutboxEvent.PublicationStatus.FAILED)
        );
    }
}