package com.example.demo.ratelimiter;

/**
 * Service interface for rate limiting operations.
 */
public interface RateLimitService {

    /**
     * Result of a rate limit check.
     */
    record Result(boolean allowed, long remainingTokens, long limit, long retryAfterSeconds) {}

    /**
     * Checks if a request is allowed under the rate limit.
     *
     * @param key      the identifier for the entity being rate-limited (e.g., user ID, IP address)
     * @param policy   the identifier for the rate limit policy (e.g., "API_CALL", "LOGIN")
     * @param capacity the maximum number of tokens allowed in the bucket
     * @param refillRate the number of tokens to add per second
     * @return the result containing allowance decision and metadata
     */
    Result consumePermission(String key, String policy, long capacity, double refillRate);
}