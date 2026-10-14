package com.example.demo.notification;

import com.example.demo.metrics.ObservabilityMetricsService;
import com.example.demo.notification.dto.CreateNotificationRequest;
import com.example.demo.notification.dto.NotificationResponse;
import com.example.demo.notification.outbox.OutboxEvent;
import com.example.demo.notification.outbox.OutboxEventRepository;
import com.example.demo.notification.outbox.OutboxPublisher;
import com.example.demo.notification.kafka.producer.NotificationEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the NotificationService.
 * Handles notification creation, persistence, and retrieval.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    private final ObservabilityMetricsService metricsService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   OutboxEventRepository outboxEventRepository,
                                   OutboxPublisher outboxPublisher,
                                   ObservabilityMetricsService metricsService) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.metricsService = Objects.requireNonNull(metricsService);
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(String userId, CreateNotificationRequest request) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

        // Check for idempotency if key is provided
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
            return notificationRepository.findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey())
                    .map(existing -> {
                        log.info("Returning existing notification for idempotency key: {} and user: {}",
                                request.getIdempotencyKey(), userId);
                        return new NotificationResponse(existing);
                    })
                    .orElseGet(() -> createNewNotification(userId, request));
        } else {
            return createNewNotification(userId, request);
        }
    }

    private NotificationResponse createNewNotification(String userId, CreateNotificationRequest request) {
        Notification notification = new Notification(
                userId,
                convertToNotificationType(request.getType()),
                request.getRecipient(),
                request.getTemplateId(),
                request.getPayload(),
                request.getIdempotencyKey()
        );

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Created notification with ID: {} for user: {}", savedNotification.getId(), userId);

        // Record that a notification was created
        metricsService.recordNotificationCreated();

        // Create and save outbox event for notification creation
        String eventId = UUID.randomUUID().toString();
        OutboxEvent outboxEvent = new OutboxEvent(
                eventId,
                savedNotification.getId(),
                "NOTIFICATION_CREATED",
                /* In a real implementation, we would serialize the notification to JSON here */
                /* For simplicity, we're using a placeholder - the actual implementation would */
                /* depend on how we want to structure our events */
                "{\"notificationId\": " + savedNotification.getId() + "}"
        );
        outboxEventRepository.save(outboxEvent);
        log.info("Created outbox event {} for notification {}", eventId, savedNotification.getId());

        return new NotificationResponse(savedNotification);
    }

    private Notification.NotificationType convertToNotificationType(CreateNotificationRequest.NotificationType type) {
        return switch (type) {
            case EMAIL -> Notification.NotificationType.EMAIL;
            case SMS -> Notification.NotificationType.SMS;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(String userId, Long notificationId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(notificationId, "Notification ID cannot be null");

        return notificationRepository.findById(notificationId)
                .filter(notification -> notification.getUserId().equals(userId))
                .map(NotificationResponse::new)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification not found with ID: " + notificationId + " for user: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(pageable, "Pageable cannot be null");

        List<Notification> notifications = notificationRepository.findByUserId(userId);
        List<NotificationResponse> notificationResponses = notifications.stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), notificationResponses.size());
        List<NotificationResponse> pageContent = notificationResponses.subList(start, end);

        return new PageImpl<>(pageContent, pageable, notificationResponses.size());
    }
}