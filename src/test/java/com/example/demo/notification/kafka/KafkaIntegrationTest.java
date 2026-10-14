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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@EmbeddedKafka(partitions = 1, topics = {"notification-events"})
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"));

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @InjectMocks
    private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Test
    void shouldSendNotificationEventToKafkaTopic() {
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
        verify(kafkaTemplate).send(eq("notification-events"), any(), any());
        // More specific verification would require capturing the arguments
        // For now, we verify that send was called with the topic name
    }

    @Test
    void shouldProcessNotificationEventFromKafkaAndUpdateStatus() {
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
                java.util.UUID.randomUUID().toString(),
                notification.getId(),
                "NOTIFICATION_CREATED",
                Instant.now(),
                notification
        );

        // When
        notificationEventConsumer.consumeNotificationEvent(createdEvent);

        // Then
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(Notification.NotificationStatus.QUEUED);
    }
}