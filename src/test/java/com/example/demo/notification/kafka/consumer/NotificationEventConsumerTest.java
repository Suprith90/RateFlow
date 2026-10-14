package com.example.demo.notification.kafka.consumer;

import com.example.demo.notification.Notification;
import com.example.demo.notification.NotificationRepository;
import com.example.demo.notification.Notification.NotificationStatus;
import com.example.demo.notification.kafka.NotificationEvent;
import com.example.demo.notification.provider.MockEmailProvider;
import com.example.demo.notification.provider.MockSMSProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MockEmailProvider emailProvider;

    @Mock
    private MockSMSProvider smsProvider;

    @InjectMocks
    private NotificationEventConsumer notificationEventConsumer;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

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
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        verify(emailProvider).sendEmail(notification);
    }

    @Test
    void shouldProcessNotificationCreatedEventForSms() {
        // Given
        Notification notification = new Notification(
                1L,
                "user123",
                Notification.NotificationType.SMS,
                "+1234567890",
                "sms-welcome-template",
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
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        verify(smsProvider).sendSMS(notification);
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
        notification.setStatus(NotificationStatus.QUEUED);

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
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void shouldProcessSmsSentEventAndUpdateStatusToSent() {
        // Given
        Notification notification = new Notification(
                1L,
                "user123",
                Notification.NotificationType.SMS,
                "+1234567890",
                "sms-welcome-template",
                "{\"name\": \"John\"}",
                "idempotency-key-123"
        );
        notification.setStatus(NotificationStatus.QUEUED);

        NotificationEvent sentEvent = new NotificationEvent(
                UUID.randomUUID().toString(),
                notification.getId(),
                "SMS_SENT",
                Instant.now(),
                notification
        );

        // When
        notificationEventConsumer.consumeNotificationEvent(sentEvent);

        // Then
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(NotificationStatus.SENT);
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
        notification.setStatus(NotificationStatus.QUEUED);

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
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
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
        notification.setStatus(NotificationStatus.QUEUED);

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
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(NotificationStatus.RETRY_PENDING);
        verify(emailProvider).sendEmailWithRetry(notification);
    }

    @Test
    void shouldProcessSmsRetryEventAndUpdateStatusToRetryPending() {
        // Given
        Notification notification = new Notification(
                1L,
                "user123",
                Notification.NotificationType.SMS,
                "+1234567890",
                "sms-welcome-template",
                "{\"name\": \"John\"}",
                "idempotency-key-123"
        );
        notification.setStatus(NotificationStatus.QUEUED);

        NotificationEvent retryEvent = new NotificationEvent(
                UUID.randomUUID().toString(),
                notification.getId(),
                "SMS_RETRY",
                Instant.now(),
                notification
        );

        // When
        notificationEventConsumer.consumeNotificationEvent(retryEvent);

        // Then
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getStatus()).isEqualTo(NotificationStatus.RETRY_PENDING);
        verify(smsProvider).sendSMSWithRetry(notification);
    }
}