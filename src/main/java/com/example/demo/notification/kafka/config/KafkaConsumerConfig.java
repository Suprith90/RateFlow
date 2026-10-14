package com.example.demo.notification.kafka.config;

import com.example.demo.notification.kafka.NotificationEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    /**
     * Consumer factory configuration.
     * @return ConsumerFactory for String keys and NotificationEvent values
     */
    @Bean
    public ConsumerFactory<String, NotificationEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"); // In production, this would come from application.properties
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "notification-processor");
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        // Consumer configuration for reliability and idempotency
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");

        // Configure JsonDeserializer to trust the NotificationEvent class
        JsonDeserializer<NotificationEvent> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.addTrustedPackages("com.example.demo.notification.kafka");
        jsonDeserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    /**
     * Concurrent Kafka listener container factory.
     * @return factory for creating listener containers
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Configure concurrency and other settings
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }
}