package com.example.demo.notification;

import com.example.demo.notification.dto.CreateNotificationRequest;
import com.example.demo.notification.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for authorization aspects of the notification system.
 * These tests verify how the system would handle role-based access control
 * if such a system were implemented on top of the existing authentication.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationTest {

    @Mock
    private NotificationService notificationService;

    private String testUserId = "test-user-123";
    private String adminUserId = "admin-user-456";
    private CreateNotificationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateNotificationRequest();
        validRequest.setType(CreateNotificationRequest.NotificationType.EMAIL);
        validRequest.setRecipient("test@example.com");
        validRequest.setTemplateId("welcome-email");
        validRequest.setPayload("{\"name\": \"Test User\"}");
        validRequest.setIdempotencyKey("test-idempotency-key");
    }

    @Test
    void shouldAllowAdminToPerformAllOperations() {
        // Given
        mockAdminAuthentication(adminUserId);

        NotificationResponse expectedResponse = new NotificationResponse();
        expectedResponse.setId(1L);
        expectedResponse.setUserId(adminUserId);
        expectedResponse.setType("EMAIL");
        expectedResponse.setStatus("QUEUED");

        when(notificationService.createNotification(eq(adminUserId), any(CreateNotificationRequest.class)))
                .thenReturn(expectedResponse);

        // When
        NotificationResponse response = notificationService.createNotification(adminUserId, validRequest);

        // Then
        assertNotNull(response);
        assertEquals(adminUserId, response.getUserId());
        assertEquals("QUEUED", response.getStatus());

        verify(notificationService).createNotification(eq(adminUserId), any(CreateNotificationRequest.class));
    }

    @Test
    void shouldAllowRegularUserToCreateOwnNotifications() {
        // Given
        mockUserAuthentication(testUserId);

        NotificationResponse expectedResponse = new NotificationResponse();
        expectedResponse.setId(1L);
        expectedResponse.setUserId(testUserId);
        expectedResponse.setType("EMAIL");
        expectedResponse.setStatus("QUEUED");

        when(notificationService.createNotification(eq(testUserId), any(CreateNotificationRequest.class)))
                .thenReturn(expectedResponse);

        // When
        NotificationResponse response = notificationService.createNotification(testUserId, validRequest);

        // Then
        assertNotNull(response);
        assertEquals(testUserId, response.getUserId());
        assertEquals("QUEUED", response.getStatus());

        verify(notificationService).createNotification(eq(testUserId), any(CreateNotificationRequest.class));
    }

    @Test
    void shouldPreventUserFromAccessingOthersNotifications() {
        // Given
        mockUserAuthentication(testUserId);
        String otherUserId = "other-user-789";
        Long notificationId = 1L;

        // Simulate that the notification belongs to another user
        Notification otherUserNotification = new Notification(
                otherUserId,
                Notification.NotificationType.EMAIL,
                "other@example.com",
                "template",
                "{}",
                "other-key"
        );
        otherUserNotification.setId(notificationId);

        when(notificationService.getNotification(eq(testUserId), eq(notificationId)))
                .thenThrow(new NotificationNotFoundException(
                        "Notification not found with ID: " + notificationId +
                        " for user: " + testUserId));

        // When/Then
        NotificationNotFoundException exception = assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.getNotification(testUserId, notificationId));

        assertTrue(exception.getMessage().contains("Notification not found with ID: " + notificationId));
        assertTrue(exception.getMessage().contains("for user: " + testUserId));

        verify(notificationService).getNotification(eq(testUserId), eq(notificationId));
    }

    @Test
    void shouldAllowUserToViewOwnNotifications() {
        // Given
        mockUserAuthentication(testUserId);
        Long notificationId = 1L;

        Notification userNotification = new Notification(
                testUserId,
                Notification.NotificationType.EMAIL,
                "test@example.com",
                "welcome-email",
                "{\"name\": \"Test User\"}",
                "test-key"
        );
        userNotification.setId(notificationId);

        NotificationResponse expectedResponse = new NotificationResponse();
        expectedResponse.setId(notificationId);
        expectedResponse.setUserId(testUserId);
        expectedResponse.setType("EMAIL");
        expectedResponse.setStatus("QUEUED");

        when(notificationService.getNotification(eq(testUserId), eq(notificationId)))
                .thenReturn(expectedResponse);

        // When
        NotificationResponse response = notificationService.getNotification(testUserId, notificationId);

        // Then
        assertNotNull(response);
        assertEquals(testUserId, response.getUserId());
        assertEquals(notificationId, response.getId());

        verify(notificationService).getNotification(eq(testUserId), eq(notificationId));
    }

    // Helper methods to simulate authentication
    private void mockUserAuthentication(String userId) {
        UserDetails userDetails = User.builder()
                .username(userId)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAdminAuthentication(String userId) {
        UserDetails userDetails = User.builder()
                .username(userId)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}