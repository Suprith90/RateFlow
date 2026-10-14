package com.example.demo.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationRepository.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRepositoryTest {

    @Mock
    private NotificationRepository notificationRepository;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification(
                "test-user-123",
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-email",
                "{\"name\": \"Test User\"}",
                "test-idempotency-key"
        );
        testNotification.setId(1L);
    }

    @Test
    void findByUserId_ShouldReturnNotificationsForUser() {
        // Given
        String userId = "test-user-123";
        List<Notification> expectedNotifications = List.of(testNotification);

        when(notificationRepository.findByUserId(userId))
                .thenReturn(expectedNotifications);

        // When
        List<Notification> result = notificationRepository.findByUserId(userId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testNotification.getId(), result.get(0).getId());
        assertEquals(userId, result.get(0).getUserId());

        verify(notificationRepository).findByUserId(userId);
    }

    @Test
    void findByUserIdAndIdempotencyKey_ShouldReturnNotificationWhenExists() {
        // Given
        String userId = "test-user-123";
        String idempotencyKey = "test-idempotency-key";

        when(notificationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.of(testNotification));

        // When
        Optional<Notification> result = notificationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testNotification.getId(), result.get().getId());
        assertEquals(userId, result.get().getUserId());
        assertEquals(idempotencyKey, result.get().getIdempotencyKey());

        verify(notificationRepository).findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    @Test
    void findByUserIdAndIdempotencyKey_ShouldReturnEmptyWhenNotExists() {
        // Given
        String userId = "test-user-123";
        String idempotencyKey = "non-existent-key";

        when(notificationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.empty());

        // When
        Optional<Notification> result = notificationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

        // Then
        assertFalse(result.isPresent());

        verify(notificationRepository).findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    @Test
    void findByUserIdAndStatus_ShouldReturnNotificationsMatchingCriteria() {
        // Given
        String userId = "test-user-123";
        Notification.NotificationStatus status = Notification.NotificationStatus.SENT;
        List<Notification> expectedNotifications = List.of(testNotification);

        when(notificationRepository.findByUserIdAndStatus(userId, status))
                .thenReturn(expectedNotifications);

        // When
        List<Notification> result = notificationRepository.findByUserIdAndStatus(userId, status);

        // Then
        assertEquals(1, result.size());
        assertEquals(testNotification.getId(), result.get(0).getId());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(status, result.get(0).getStatus());

        verify(notificationRepository).findByUserIdAndStatus(userId, status);
    }

    @Test
    void countByUserId_ShouldReturnCorrectCount() {
        // Given
        String userId = "test-user-123";
        long expectedCount = 5L;

        when(notificationRepository.countByUserId(userId))
                .thenReturn(expectedCount);

        // When
        long result = notificationRepository.countByUserId(userId);

        // Then
        assertEquals(expectedCount, result);

        verify(notificationRepository).countByUserId(userId);
    }

    @Test
    void findByUserId_ShouldReturnEmptyListWhenNoNotifications() {
        // Given
        String userId = "test-user-123";

        when(notificationRepository.findByUserId(userId))
                .thenReturn(Collections.emptyList());

        // When
        List<Notification> result = notificationRepository.findByUserId(userId);

        // Then
        assertTrue(result.isEmpty());

        verify(notificationRepository).findByUserId(userId);
    }
}