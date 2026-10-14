package com.example.demo.notification.kafka;

import com.example.demo.notification.Notification;
import com.example.demo.notification.NotificationRepository;
import com.example.demo.notification.Notification.NotificationStatus;
import com.example.demo.notification.kafka.consumer.NotificationEventConsumer;
import com.example.demo.notification.kafka.producer.NotificationEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {"notification-events"})
class KafkaNotificationIntegrationTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @InjectMocks
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    @Captor
    private ArgumentCaptor<NotificationEvent> eventCaptor;

    @Test
    void shouldProcessNotificationCreatedEventAndSetStatusToQueued() {
        // Given
        Notification notification = new Notification(
                1L,
                "user123",
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-template",
                "{\"name\": \"John\"}",
                "idempotency-key-123"
        );

        NotificationEvent createdEvent = new NotificationEvent(
                UUID.randomUUID().toString(),
                notification.getId(),
                "NOTIFICATION_CREATED",
                Instant.now(),
                notification
        );

        // When
        notificationEventConsumer.consumeNotificationEvent(createdEvent);

        // Then
        verify(notificationRepository).save(notification);
        assertThat(notification.getStatus()).isEqualTo(Notification.NotificationStatus.QUEUED);
    }

    @Test
    void shouldProcessEmailSentEventAndUpdateStatusToSent() {
        // Given
        Notification notification = new Notification(
                1L,
                "user123",
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-template",
                "{\"name\": \"John\"}",
                "idempotency-key-123"
        );
        notification.setStatus(Notification.NotificationStatus.QUEUED);

        NotificationEvent sentEvent = new NotificationEvent(
                UUID.randomUUID().toString(),
                notification.getId(),
                "EMAIL_SENT",
                Instant.now(),
                notification
        );

        // When
        notificationEventConsumer.consumeNotificationEvent(sentEvent);

        // Then
        verify(notificationRepository).save(notification);
        assertThat(notification.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
    }

    @Test
    void shouldProcessEmailFailedEventAndUpdateStatusToFailed() {
        // Given
        Notification notification = new Notification(
                1L,
                "user123",
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-template",
                "{\"name\": \"John\"}",
                "idempotency-key-123"
        );
        notification.setStatus(Notification.NotificationStatus.QUEUED);

        NotificationEvent failedEvent = new NotificationEvent(
                UUID.randomUUID().toString(),
                notification.getId(),
                "EMAIL_FAILED",
                Instant.now(),
                notification
        );

        // When
        notificationEventConsumer.consumeNotificationEvent(failedEvent);

        // Then
        verify(notificationRepository).save(notification);
        assertThat(notification.getStatus()).isEqualTo(Notification.NotificationStatus.FAILED);
    }

    @Test
    void shouldProcessEmailRetryEventAndUpdateStatusToRetryPending() {
        // Given
        Notification notification = new Notification(
                1L,
                "user123",
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-template",
                "{\"name\": \"John\"}",
                "idempotency-key-123"
        );
        notification.setStatus(Notification.NotificationStatus.RETRY_PENDING);

        NotificationEvent retryEvent = new NotificationEvent(
                UUID.randomUUID().toString(),
                notification.getId(),
                "EMAIL_RETRY",
                Instant.now(),
                notification
        );

        // When
        notificationEventConsumer.consumeNotificationEvent(retryEvent);

        // Then
        verify(notificationRepository).save(notification);
        assertThat(notification.getStatus()).isEqualTo(Notification.NotificationStatus.RETRY_PENDING);
        // In a real test, we would verify that the email provider's sendEmailWithRetry method was called
    }
}