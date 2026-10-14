package com.example.demo.config;

import com.example.demo.notification.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application metrics.
 * <p>
 * This class sets up custom metrics for the RateFlow application,
 * including rate limiting metrics, notification metrics, and outbox metrics.
 * <p>
 * Gauges are configured to query the repository directly for current values.
 */
@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry meterRegistry, OutboxEventRepository outboxEventRepository) {
        // Rate limiter metrics
        Counter.builder("ratelimit.requests_allowed_total")
                .description("Total number of requests allowed by rate limiter")
                .register(meterRegistry);

        Counter.builder("ratelimit.requests_rejected_total")
                .description("Total number of requests rejected by rate limiter")
                .register(meterRegistry);

        Timer.builder("ratelimit.decision_latency_seconds")
                .description("Latency of rate limit decisions")
                .register(meterRegistry);

        // Notification metrics
        Counter.builder("notifications_created_total")
                .description("Total number of notifications created")
                .register(meterRegistry);

        Counter.builder("notifications_delivered_success_total")
                .description("Total number of notifications successfully delivered")
                .register(meterRegistry);

        Counter.builder("notifications_delivered_failed_total")
                .description("Total number of notifications that failed delivery")
                .register(meterRegistry);

        Timer.builder("notification.processing_duration_seconds")
                .description("Duration of notification processing")
                .register(meterRegistry);

        // Outbox metrics - gauges that query the repository directly
        io.micrometer.core.instrument.Gauge.builder("outbox.pending_events", outboxEventRepository,
                OutboxEventRepository::countPendingEvents)
                .description("Number of pending events in the outbox")
                .register(meterRegistry);

        io.micrometer.core.instrument.Gauge.builder("outbox.failed_events", outboxEventRepository,
                OutboxEventRepository::countFailedEvents)
                .description("Number of failed events in the outbox")
                .register(meterRegistry);

        io.micrometer.core.instrument.Gauge.builder("outbox.events_retry_count", outboxEventRepository,
                OutboxEventRepository::averageRetryCount)
                .description("Average retry count for outbox events")
                .register(meterRegistry);
    }
}