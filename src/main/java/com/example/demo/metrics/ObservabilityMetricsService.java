package com.example.demo.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for recording application observability metrics.
 * <p>
 * This service provides methods to record metrics for rate limiting,
 * notifications, and outbox operations as specified in the requirements.
 */
@Service
public class ObservabilityMetricsService {

    private final Counter rateLimitAllowedCounter;
    private final Counter rateLimitRejectedCounter;
    private final Timer rateLimitDecisionTimer;

    private final Counter notificationsCreatedCounter;
    private final Counter notificationsDeliveredSuccessCounter;
    private final Counter notificationsDeliveredFailedCounter;
    private final Timer notificationProcessingTimer;

    public ObservabilityMetricsService(MeterRegistry meterRegistry) {
        // Rate limiter metrics
        this.rateLimitAllowedCounter = Counter.builder("ratelimit.requests_allowed_total")
                .description("Total number of requests allowed by rate limiter")
                .register(meterRegistry);

        this.rateLimitRejectedCounter = Counter.builder("ratelimit.requests_rejected_total")
                .description("Total number of requests rejected by rate limiter")
                .register(meterRegistry);

        this.rateLimitDecisionTimer = Timer.builder("ratelimit.decision_latency_seconds")
                .description("Latency of rate limit decisions")
                .register(meterRegistry);

        // Notification metrics
        this.notificationsCreatedCounter = Counter.builder("notifications_created_total")
                .description("Total number of notifications created")
                .register(meterRegistry);

        this.notificationsDeliveredSuccessCounter = Counter.builder("notifications_delivered_success_total")
                .description("Total number of notifications successfully delivered")
                .register(meterRegistry);

        this.notificationsDeliveredFailedCounter = Counter.builder("notifications_delivered_failed_total")
                .description("Total number of notifications that failed delivery")
                .register(meterRegistry);

        this.notificationProcessingTimer = Timer.builder("notification.processing_duration_seconds")
                .description("Duration of notification processing")
                .register(meterRegistry);
    }

    /**
     * Records a rate limit allowed decision.
     */
    public void recordRateLimitAllowed() {
        rateLimitAllowedCounter.increment();
    }

    /**
     * Records a rate limit rejected decision.
     */
    public void recordRateLimitRejected() {
        rateLimitRejectedCounter.increment();
    }

    /**
     * Records the latency of a rate limit decision.
     *
     * @param durationSeconds the duration in seconds
     */
    public void recordRateLimitDecisionLatency(double durationSeconds) {
        rateLimitDecisionTimer.record(Duration.ofSeconds((long) durationSeconds));
    }

    /**
     * Records that a notification was created.
     */
    public void recordNotificationCreated() {
        notificationsCreatedCounter.increment();
    }

    /**
     * Records that a notification was successfully delivered.
     */
    public void recordNotificationDeliveredSuccess() {
        notificationsDeliveredSuccessCounter.increment();
    }

    /**
     * Records that a notification failed to be delivered.
     */
    public void recordNotificationDeliveredFailed() {
        notificationsDeliveredFailedCounter.increment();
    }

    /**
     * Records the duration of notification processing.
     *
     * @param durationSeconds the duration in seconds
     */
    public void recordNotificationProcessingDuration(double durationSeconds) {
        notificationProcessingTimer.record(Duration.ofSeconds((long) durationSeconds));
    }
}