package com.example.demo.notification;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Notification entities.
 * Provides CRUD operations and custom queries for notification management.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find notifications by user ID with pagination.
     * @param userId the user ID to filter by
     * @param pageable pagination information
     * @return page of notifications for the user
     */
    List<Notification> findByUserId(String userId);

    /**
     * Find a notification by user ID and idempotency key.
     * @param userId the user ID
     * @param idempotencyKey the idempotency key
     * @return optional notification if found
     */
    Optional<Notification> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    /**
     * Find notifications by user ID and status.
     * @param userId the user ID
     * @param status the notification status
     * @return list of matching notifications
     */
    List<Notification> findByUserIdAndStatus(String userId, Notification.NotificationStatus status);

    /**
     * Count notifications by user ID.
     * @param userId the user ID
     * @return count of notifications for the user
     */
    long countByUserId(String userId);
}