package com.example.demo.notification.kafka.producer;

import com.example.demo.notification.Notification;
import com.example.demo.notification.kafka.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventProducerTest {

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @InjectMocks
    private NotificationEventProducer notificationEventProducer;

    @Captor
    private ArgumentCaptor<NotificationEvent> eventCaptor;

    @Test
    void shouldPublishNotificationCreatedEvent() {
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

        // When
        notificationEventProducer.publishNotificationCreated(notification);

        // Then
        verify(kafkaTemplate).send(eq("notification-events"), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(capturedEvent.getNotificationId()).isEqualTo(1L);
        assertThat(capturedEvent.getNotificationType()).isEqualTo("EMAIL");
        assertThat(capturedEvent.getRecipient()).isEqualTo("test@example.com");
        assertThat(capturedEvent.getTemplateId()).isEqualTo("welcome-template");
        assertThat(capturedEvent.getPayload()).isEqualTo("{\"name\": \"John\"}");
    }
}