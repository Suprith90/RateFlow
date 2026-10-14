package com.example.demo.ratelimiter;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;

/**
 * Redis-backed Token Bucket rate limiter implementation.
 * <p>
 * Uses a Lua script to perform atomic token bucket operations:
 * - Read current token balance and last refill timestamp
 * - Calculate tokens to add based on elapsed time
 * - Cap at capacity
 * - Consume one token if allowed
 * - Save updated state with TTL
 */
@Service
public class TokenBucketService implements RateLimitService {

    private static final String BUCKET_KEY_PREFIX = "rate_limit:token_bucket";
    private static final int DEFAULT_TTL_SECONDS = 3600; // 1 hour

    final RedisScript<Result> consumeScript;
    final RedisTemplate<String, Object> redisTemplate;

    public TokenBucketService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.consumeScript = new DefaultRedisScript<>(LUAS_SCRIPT, Result.class);
    }

    @Override
    public Result consumePermission(String key, String policy, long capacity, double refillRate) {
        String bucketKey = BUCKET_KEY_PREFIX + ":" + key + ":" + policy;

        try {
            // Execute the Lua script atomically using RedisTemplate
            Object result = redisTemplate.execute(
                    consumeScript,
                    Collections.singletonList(bucketKey),
                    String.valueOf(capacity),
                    String.valueOf(refillRate),
                    String.valueOf(System.currentTimeMillis())
            );

            if (result instanceof Result) {
                return (Result) result;
            } else {
                // Fallback: if Redis returns unexpected result, allow the request
                // This follows the fail-open principle for rate limiting
                return new Result(true, capacity, capacity, 0);
            }
        } catch (DataAccessException e) {
            // Redis unavailable - fail open to allow requests through
            // In production, you might want to log this and/or use a circuit breaker
            return new Result(true, capacity, capacity, 0);
        }
    }

    // Lua script for atomic token bucket operations
    // Keys[1]: bucket key
    // ARGV[1]: capacity (long)
    // ARGV[2]: refillRate (double as string)
    // ARGV[3]: current timestamp milliseconds (long)
    private static final String LUAS_SCRIPT =
            "local key = KEYS[1]\n" +
            "local capacity = tonumber(ARGV[1])\n" +
            "local refillRate = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "\n" +
            "-- Get current bucket state\n" +
            "local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')\n" +
            "local tokens = tonumber(bucket[1]) or capacity\n" +
            "local lastRefill = tonumber(bucket[2]) or now\n" +
            "\n" +
            "-- Calculate elapsed time and refill tokens\n" +
            "local elapsed = (now - lastRefill) / 1000.0 -- convert to seconds\n" +
            "local newTokens = tokens + (elapsed * refillRate)\n" +
            "\n" +
            "-- Cap at capacity\n" +
            "if newTokens > capacity then\n" +
            "    newTokens = capacity\n" +
            "end\n" +
            "\n" +
            "-- Check if request can be allowed\n" +
            "local allowed = 0\n" +
            "local retryAfter = 0\n" +
            "if newTokens >= 1 then\n" +
            "    -- Consume one token\n" +
            "    newTokens = newTokens - 1\n" +
            "    allowed = 1\n" +
            "else\n" +
            "    -- Calculate retry-after time\n" +
            "    local deficit = 1 - newTokens\n" +
            "    retryAfter = math.ceil(deficit / refillRate)\n" +
            "end\n" +
            "\n" +
            "-- Save updated state\n" +
            "redis.call('HMSET', key, 'tokens', newTokens, 'last_refill', now)\n" +
            "redis.call('EXPIRE', key, " + DEFAULT_TTL_SECONDS + ")\n" +
            "\n" +
            "-- Return result as a structured format\n" +
            "-- We'll return a JSON-like string that Spring Data Redis can map to Result record\n" +
            "return {allowed, newTokens, capacity, retryAfter}";
}