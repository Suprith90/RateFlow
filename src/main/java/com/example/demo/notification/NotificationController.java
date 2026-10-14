package com.example.demo.notification;

import com.example.demo.notification.dto.CreateNotificationRequest;
import com.example.demo.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;

/**
 * REST controller for notification management.
 * Provides endpoints for creating and retrieving notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Create a new notification.
     * Requires authentication. The user ID is derived from the authenticated principal.
     *
     * @param principal the authenticated user principal
     * @param request the notification creation request
     * @return HTTP 202 Accepted with the created notification
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            Principal principal,
            @Valid @RequestBody CreateNotificationRequest request) {

        String userId = extractUserId(principal);
        log.info("Creating notification for user: {}", userId);

        NotificationResponse response = notificationService.createNotification(userId, request);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get a specific notification by ID.
     * Requires authentication. Users can only access their own notifications.
     *
     * @param principal the authenticated user principal
     * @param id the notification ID
     * @return the notification if found and owned by the user
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(
            Principal principal,
            @PathVariable Long id) {

        String userId = extractUserId(principal);
        log.info("Fetching notification {} for user: {}", id, userId);

        NotificationResponse response = notificationService.getNotification(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get notifications for the authenticated user with pagination.
     * Requires authentication. Users can only access their own notifications.
     *
     * @param principal the authenticated user principal
     * @param pageable pagination information
     * @return page of notifications for the user
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Principal principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        String userId = extractUserId(principal);
        log.info("Fetching notifications for user: {} with page: {}", userId, pageable.getPageNumber());

        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Extracts the user ID from the authentication principal.
     * In a real application, this would integrate with your security system.
     *
     * @param principal the authentication principal
     * @return the user ID
     */
    private String extractUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User must be authenticated");
        }

        // For demonstration, we'll use the principal name directly
        // In a real app, you might extract this from a JWT, session, or user details
        String userId = principal.getName();
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("Unable to determine user ID from principal");
        }
        return userId;
    }
}