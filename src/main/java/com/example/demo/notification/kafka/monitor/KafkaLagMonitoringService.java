package com.example.demo.notification.kafka.monitor;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.JsonDeserializer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.springframework.stereotype.Service;

import com.example.demo.notification.kafka.NotificationEvent;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service for monitoring Kafka consumer lag.
 * <p>
 * This service periodically checks the lag for the notification processor consumer group
 * and exposes it as a metric for monitoring systems.
 */
@Service
public class KafkaLagMonitoringService {

    private final Gauge consumerLagGauge;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final String bootstrapServers;
    private final String groupId;
    private final String topic;

    public KafkaLagMonitoringService(MeterRegistry meterRegistry) {
        // In a real application, these would come from configuration
        this.bootstrapServers = "localhost:9092";
        this.groupId = "notification-processor";
        this.topic = "notification-events";

        // Create the gauge that will report the current lag
        this.consumerLagGauge = Gauge.builder("kafka.consumer_lag", this::getConsumerLag)
                .description("Current Kafka consumer lag for notification-processor group")
                .register(meterRegistry);

        // Start periodic updates (every 30 seconds)
        scheduler.scheduleAtFixedRate(this::updateLag, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Gets the current consumer lag by querying Kafka.
     * @return the total lag across all partitions for the monitored topic and group
     */
    private double getConsumerLag() {
        try {
            return calculateLag();
        } catch (Exception e) {
            // Return -1 to indicate an error in lag calculation
            return -1;
        }
    }

    /**
     * Calculates the consumer lag by comparing current offsets with committed offsets.
     * @return the total lag across all partitions
     */
    private double calculateLag() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-lag-monitor-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, NotificationEvent> consumer =
                     new KafkaConsumer<>(props)) {

            // Get partitions for the topic
            List<TopicPartition> partitions = consumer.partitionsFor(topic)
                    .stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();

            if (partitions.isEmpty()) {
                return 0;
            }

            // Assign partitions to consumer
            consumer.assign(partitions);

            // Get end offsets (current log size)
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            // Get committed offsets (what the group has committed)
            Map<TopicPartition, OffsetAndMetadata> committedOffsets = consumer.committed(new HashSet<>(partitions));

            long totalLag = 0;
            for (TopicPartition partition : partitions) {
                long endOffset = endOffsets.getOrDefault(partition, 0L);
                OffsetAndMetadata offsetAndMetadata = committedOffsets.get(partition);
                Long committedOffset = offsetAndMetadata != null ? offsetAndMetadata.offset() : null;

                if (committedOffset != null) {
                    long lag = Math.max(0, endOffset - committedOffset);
                    totalLag += lag;
                } else {
                    // If no committed offset, lag is the entire log
                    totalLag += endOffset;
                }
            }

            return (double) totalLag;
        }
    }

    /**
     * Updates the lag metric (called by scheduler).
     * The gauge automatically calls getConsumerLag when sampled,
     * so this method exists for potential future enhancements.
     */
    private void updateLag() {
        // The gauge is passive and updates when sampled by Prometheus
        // This method exists as a hook for potential active updates
    }

    /**
     * Shuts down the scheduler when the service is destroyed.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}