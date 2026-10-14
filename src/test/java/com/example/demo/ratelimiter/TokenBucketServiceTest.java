package com.example.demo.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Unit tests for TokenBucketService.
 * Mocks Redis connections to test service behavior.
 */
class TokenBucketServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisScript<RateLimitService.Result> consumeScript;

    private TokenBucketService tokenBucketService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tokenBucketService = new TokenBucketService(redisTemplate);
        // Manually set the consumeScript since it's final
        // Using reflection to set the final field
        try {
            java.lang.reflect.Field field = TokenBucketService.class.getDeclaredField("consumeScript");
            field.setAccessible(true);
            field.set(tokenBucketService, consumeScript);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void consumePermission_fullBucket_ShouldAllow() throws Exception {
        // Setup: mock RedisTemplate.execute to return [allowed=1, remainingTokens=99, limit=100, retryAfter=0]
        when(redisTemplate.execute(
                eq(consumeScript),
                anyList(),
                any(),
                any(),
                any()
        )).thenReturn(new RateLimitService.Result(true, 99L, 100L, 0L));

        // Execute
        RateLimitService.Result result = tokenBucketService.consumePermission("user1", "API_CALL", 100, 10.0);

        // Verify the mock was called
        verify(redisTemplate, times(1)).execute(eq(consumeScript), anyList(), any(), any(), any());

        // Verify
        assertTrue(result.allowed());
        assertEquals(99, result.remainingTokens());
        assertEquals(100, result.limit());
        assertEquals(0, result.retryAfterSeconds());
    }

    @Test
    void consumePermission_emptyBucket_ShouldReject() throws Exception {
        // Setup: mock RedisTemplate.execute to return [allowed=0, remainingTokens=0, limit=100, retryAfter=10]
        when(redisTemplate.execute(
                eq(consumeScript),
                anyList(),
                any(),
                any(),
                any()
        )).thenReturn(new RateLimitService.Result(false, 0L, 100L, 10L));

        // Execute
        RateLimitService.Result result = tokenBucketService.consumePermission("user1", "API_CALL", 100, 10.0);

        // Verify the mock was called
        verify(redisTemplate, times(1)).execute(eq(consumeScript), anyList(), any(), any(), any());

        // Verify
        assertEquals(false, result.allowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(100, result.limit());
        assertEquals(10, result.retryAfterSeconds());
    }

    @Test
    void consumePermission_partialRefill_ShouldAllow() throws Exception {
        // Setup: mock RedisTemplate.execute to return [allowed=1, remainingTokens=64, limit=100, retryAfter=0]
        when(redisTemplate.execute(
                eq(consumeScript),
                anyList(),
                any(),
                any(),
                any()
        )).thenReturn(new RateLimitService.Result(true, 64L, 100L, 0L));

        // Execute
        RateLimitService.Result result = tokenBucketService.consumePermission("user1", "API_CALL", 100, 2.0);

        // Verify the mock was called
        verify(redisTemplate, times(1)).execute(eq(consumeScript), anyList(), any(), any(), any());

        // Verify
        assertTrue(result.allowed());
        assertEquals(64, result.remainingTokens());
        assertEquals(100, result.limit());
        assertEquals(0, result.retryAfterSeconds());
    }

    @Test
    void consumePermission_redisFailure_ShouldFailOpen() throws Exception {
        // Setup: simulate Redis failure by throwing DataAccessException
        when(redisTemplate.execute(
                eq(consumeScript),
                anyList(),
                any(),
                any(),
                any()
        )).thenThrow(new DataAccessException("Redis connection failed") {});

        // Execute
        RateLimitService.Result result = tokenBucketService.consumePermission("user1", "API_CALL", 100, 10.0);

        // Verify: should fail open (allow request)
        assertTrue(result.allowed());
        assertEquals(100, result.remainingTokens());
        assertEquals(100, result.limit());
        assertEquals(0, result.retryAfterSeconds());
    }
}