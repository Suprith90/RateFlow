package com.example.demo.ratelimiter;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Redis-backed Token Bucket rate limiter.
 * Checks if Redis is accessible and the rate limiting service is functional.
 */
@Component
public class RedisRateLimitHealthIndicator implements HealthIndicator {

    private final TokenBucketService rateLimitService;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRateLimitHealthIndicator(TokenBucketService rateLimitService,
                                         RedisTemplate<String, Object> redisTemplate) {
        this.rateLimitService = rateLimitService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Try to connect to Redis and perform a simple operation
            Boolean connected = (Boolean) redisTemplate.execute((RedisConnection connection) -> {
                try {
                    return connection.ping();
                } catch (Exception e) {
                    return false;
                }
            });

            if (Boolean.TRUE.equals(connected)) {
                // Test the rate limiting service with a dummy key
                String testKey = "health_check_test";
                var result = rateLimitService.consumePermission(
                        testKey, "HEALTH_CHECK", 100, 10.0);

                // Clean up the test key
                redisTemplate.delete("rate_limit:token_bucket:" + testKey + ":HEALTH_CHECK");

                return Health.up()
                        .withDetail("redis_connected", true)
                        .withDetail("rate_limit_service_operational", true)
                        .withDetail("test_result_allowed", result.allowed())
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis_connected", false)
                        .withDetail("error", "Redis connection failed")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}