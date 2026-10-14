package com.example.demo.notification;

import com.example.demo.notification.dto.CreateNotificationRequest;
import com.example.demo.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing notifications.
 * Handles creation, persistence, and retrieval of notifications.
 */
public interface NotificationService {

    /**
     * Create a new notification.
     * @param userId the ID of the user (from authenticated principal)
     * @param request the notification creation request
     * @return the created notification response
     */
    NotificationResponse createNotification(String userId, CreateNotificationRequest request);

    /**
     * Get a notification by ID.
     * @param userId the ID of the user (for ownership validation)
     * @param notificationId the ID of the notification
     * @return the notification response if found and owned by user
     */
    NotificationResponse getNotification(String userId, Long notificationId);

    /**
     * Get notifications for a user with pagination.
     * @param userId the ID of the user (for ownership validation)
     * @param pageable pagination information
     * @return page of notifications for the user
     */
    Page<NotificationResponse> getNotifications(String userId, Pageable pageable);
}