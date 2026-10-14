package com.example.demo.notification;

import com.example.demo.notification.dto.CreateNotificationRequest;
import com.example.demo.notification.dto.NotificationResponse;
import com.example.demo.notification.outbox.OutboxEvent;
import com.example.demo.notification.outbox.OutboxEventRepository;
import com.example.demo.notification.outbox.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxPublisher outboxPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private String testUserId;
    private CreateNotificationRequest validRequest;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        validRequest = new CreateNotificationRequest();
        validRequest.setType(CreateNotificationRequest.NotificationType.EMAIL);
        validRequest.setRecipient("test@example.com");
        validRequest.setTemplateId("welcome-email");
        validRequest.setPayload("{\"name\": \"Test User\"}");
        validRequest.setIdempotencyKey("test-idempotency-key");
    }

    @Test
    void createNotification_ShouldCreateNewNotificationAndSaveOutboxEvent() {
        // Arrange
        Notification expectedNotification = new Notification(
                testUserId,
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-email",
                "{\"name\": \"Test User\"}",
                "test-idempotency-key"
        );
        expectedNotification.setId(1L);

        OutboxEvent expectedOutboxEvent = new OutboxEvent(
                UUID.randomUUID().toString(),
                expectedNotification.getId(),
                "NOTIFICATION_CREATED",
                "{\"notificationId\": 1}"
        );

        when(notificationRepository.findByUserIdAndIdempotencyKey(
                eq(testUserId), eq("test-idempotency-key")))
                .thenReturn(java.util.Optional.empty());

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(expectedNotification);

        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenReturn(expectedOutboxEvent);

        // Act
        NotificationResponse response = notificationService.createNotification(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(testUserId, response.getUserId());
        assertEquals("EMAIL", response.getType());
        assertEquals("test@example.com", response.getRecipient());
        assertEquals("welcome-email", response.getTemplateId());
        assertEquals("{\"name\": \"Test User\"}", response.getPayload());
        assertEquals("QUEUED", response.getStatus());
        assertEquals("test-idempotency-key", response.getIdempotencyKey());

        // Verify repository interactions
        verify(notificationRepository).findByUserIdAndIdempotencyKey(
                eq(testUserId), eq("test-idempotency-key"));
        verify(notificationRepository).save(any(Notification.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void createNotification_WithIdempotencyKey_ReturnsExistingNotification() {
        // Arrange
        Notification existingNotification = new Notification(
                testUserId,
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-email",
                "{\"name\": \"Test User\"}",
                "test-idempotency-key"
        );
        existingNotification.setId(1L);
        existingNotification.setStatus(Notification.NotificationStatus.SENT);

        when(notificationRepository.findByUserIdAndIdempotencyKey(
                eq(testUserId), eq("test-idempotency-key")))
                .thenReturn(java.util.Optional.of(existingNotification));

        // Act
        NotificationResponse response = notificationService.createNotification(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("SENT", response.getStatus()); // Should reflect existing status

        // Verify save was NOT called
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationRepository).findByUserIdAndIdempotencyKey(
                eq(testUserId), eq("test-idempotency-key"));

        // Verify outbox event was NOT saved for existing notification
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void createNotification_WithoutIdempotencyKey_AlwaysCreatesNewAndSavesOutboxEvent() {
        // Arrange
        validRequest.setIdempotencyKey(null);

        Notification expectedNotification = new Notification(
                testUserId,
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-email",
                "{\"name\": \"Test User\"}",
                null
        );
        expectedNotification.setId(1L);

        OutboxEvent expectedOutboxEvent = new OutboxEvent(
                UUID.randomUUID().toString(),
                expectedNotification.getId(),
                "NOTIFICATION_CREATED",
                "{\"notificationId\": 1}"
        );

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(expectedNotification);

        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenReturn(expectedOutboxEvent);

        // Act
        NotificationResponse response = notificationService.createNotification(testUserId, validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertNull(response.getIdempotencyKey());

        // Verify save was called
        verify(notificationRepository, never()).findByUserIdAndIdempotencyKey(any(), any());
        verify(notificationRepository).save(any(Notification.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void getNotification_ShouldReturnNotificationWhenFoundAndOwned() {
        // Arrange
        Long notificationId = 1L;
        Notification notification = new Notification(
                testUserId,
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-email",
                "{\"name\": \"Test User\"}",
                "test-key"
        );
        notification.setId(notificationId);

        when(notificationRepository.findById(notificationId))
                .thenReturn(java.util.Optional.of(notification));

        // Act
        NotificationResponse response = notificationService.getNotification(testUserId, notificationId);

        // Assert
        assertNotNull(response);
        assertEquals(notificationId, response.getId());
        assertEquals(testUserId, response.getUserId());

        // Verify
        verify(notificationRepository).findById(notificationId);
    }

    @Test
    void getNotification_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        Long notificationId = 1L;
        when(notificationRepository.findById(notificationId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        NotificationNotFoundException exception = assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.getNotification(testUserId, notificationId));

        assertTrue(exception.getMessage().contains("Notification not found with ID: 1"));
        assertTrue(exception.getMessage().contains("for user: test-user-123"));

        // Verify
        verify(notificationRepository).findById(notificationId);
    }

    @Test
    void getNotification_ShouldThrowExceptionWhenOwnedByDifferentUser() {
        // Arrange
        Long notificationId = 1L;
        String differentUserId = "different-user-456";
        Notification notification = new Notification(
                differentUserId,
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-email",
                "{\"name\": \"Test User\"}",
                "test-key"
        );
        notification.setId(notificationId);

        when(notificationRepository.findById(notificationId))
                .thenReturn(java.util.Optional.of(notification));

        // Act & Assert
        NotificationNotFoundException exception = assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.getNotification(testUserId, notificationId));

        assertTrue(exception.getMessage().contains("Notification not found with ID: 1"));
        assertTrue(exception.getMessage().contains("for user: test-user-123"));

        // Verify
        verify(notificationRepository).findById(notificationId);
    }

    @Test
    void getNotifications_ShouldReturnPageOfNotifications() {
        // Arrange
        String userId = "test-user";
        Pageable pageable = PageRequest.of(0, 10);

        Notification notification1 = new Notification(
                userId,
                Notification.NotificationType.EMAIL,
                "test1@example.com",
                "template1",
                "{\"name\": \"User 1\"}",
                "key1"
        );
        notification1.setId(1L);

        Notification notification2 = new Notification(
                userId,
                Notification.NotificationType.SMS,
                "+1234567890",
                "template2",
                "{\"name\": \"User 2\"}",
                "key2"
        );
        notification2.setId(2L);

        List<Notification> notifications = List.of(notification1, notification2);

        when(notificationRepository.findByUserId(userId))
                .thenReturn(notifications);

        // Act
        Page<NotificationResponse> response = notificationService.getNotifications(userId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals(2, response.getContent().size());

        NotificationResponse first = response.getContent().get(0);
        assertEquals(1L, first.getId());
        assertEquals("EMAIL", first.getType());
        assertEquals("test1@example.com", first.getRecipient());

        NotificationResponse second = response.getContent().get(1);
        assertEquals(2L, second.getId());
        assertEquals("SMS", second.getType());
        assertEquals("+1234567890", second.getRecipient());

        // Verify
        verify(notificationRepository).findByUserId(userId);
    }
}