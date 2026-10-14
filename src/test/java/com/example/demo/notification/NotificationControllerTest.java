package com.example.demo.notification;

import com.example.demo.notification.dto.CreateNotificationRequest;
import com.example.demo.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationController.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private String testUserId = "test-user-123";
    private CreateNotificationRequest validRequest;
    private NotificationResponse sampleResponse;

    private CreateNotificationRequest getValidRequest() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setType(CreateNotificationRequest.NotificationType.EMAIL);
        request.setRecipient("test@example.com");
        request.setTemplateId("welcome-email");
        request.setPayload("{\"name\": \"Test User\"}");
        request.setIdempotencyKey("test-idempotency-key");
        return request;
    }

    private NotificationResponse getSampleResponse() {
        NotificationResponse response = new NotificationResponse();
        response.setId(1L);
        response.setUserId(testUserId);
        response.setType("EMAIL");
        response.setRecipient("test@example.com");
        response.setTemplateId("welcome-email");
        response.setPayload("{\"name\": \"Test User\"}");
        response.setStatus("QUEUED");
        response.setIdempotencyKey("test-idempotency-key");
        return response;
    }

    @Test
    void createNotification_ShouldReturnAcceptedStatus() {
        // Arrange
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(testUserId);
        when(notificationService.createNotification(eq(testUserId), any(CreateNotificationRequest.class)))
                .thenReturn(getSampleResponse());

        // Act
        ResponseEntity<NotificationResponse> response = notificationController.createNotification(mockPrincipal, getValidRequest());

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(testUserId, response.getBody().getUserId());
        assertEquals("EMAIL", response.getBody().getType());

        // Verify
        verify(notificationService).createNotification(eq(testUserId), any(CreateNotificationRequest.class));
    }

    @Test
    void getNotification_ShouldReturnOkStatus() {
        // Arrange
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(testUserId);
        Long notificationId = 1L;
        when(notificationService.getNotification(eq(testUserId), eq(notificationId)))
                .thenReturn(getSampleResponse());

        // Act
        ResponseEntity<NotificationResponse> response = notificationController.getNotification(mockPrincipal, notificationId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(testUserId, response.getBody().getUserId());

        // Verify
        verify(notificationService).getNotification(eq(testUserId), eq(notificationId));
    }

    @Test
    void getNotifications_ShouldReturnOkStatusWithPage() {
        // Arrange
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn(testUserId);
        Page<NotificationResponse> mockPage = new PageImpl<>(Collections.singletonList(getSampleResponse()));
        when(notificationService.getNotifications(eq(testUserId), any()))
                .thenReturn(mockPage);

        // Act
        ResponseEntity<Page<NotificationResponse>> response = notificationController.getNotifications(mockPrincipal, PageRequest.of(0, 10));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());

        // Verify
        verify(notificationService).getNotifications(eq(testUserId), any());
    }

    @Test
    void createNotification_ShouldThrowExceptionWhenUnauthenticated() {
        // Arrange
        Principal nullPrincipal = null;

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> notificationController.createNotification(nullPrincipal, getValidRequest()));

        assertTrue(exception.getMessage().contains("User must be authenticated"));

        // Verify service was not called
        verify(notificationService, never()).createNotification(anyString(), any());
    }
}