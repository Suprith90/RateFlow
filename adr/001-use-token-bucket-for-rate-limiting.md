# ADR 001: Use Token Bucket for Rate Limiting

## Status
Accepted

## Date
2026-10-14

## Context
We need to implement rate limiting for the API to protect against overuse and ensure fair resource allocation among users. Several algorithms are available for rate limiting, including Fixed Window, Sliding Window, Leaky Bucket, and Token Bucket.

## Decision
We chose the Token Bucket algorithm for implementing rate limiting in RateFlow.

## Consequences

### Pros
- **Burst Tolerance**: Allows users to accumulate tokens up to the bucket capacity, enabling handling of bursty traffic patterns
- **Smooth Traffic Shaping**: Provides more predictable request processing compared to Fixed Window algorithms
- **Flexibility**: Easy to adjust both the bucket capacity (burst size) and refill rate (sustained rate) independently
- **Well-Understood**: Widely implemented and understood algorithm with clear behavior characteristics

### Cons
- **State Management**: Requires maintaining bucket state per user (tokens count and last update time)
- **Complexity**: Slightly more complex to implement than Fixed Window algorithms
- **Memory Overhead**: Needs storage for each user's bucket state (mitigated by using Redis with efficient data structures)

### Implementation Details
- **Storage**: Redis is used to store bucket state for all users, enabling distributed rate limiting across multiple API instances
- **Atomicity**: Lua scripts ensure atomic check-and-update operations to prevent race conditions
- **Configuration**: 
  - Bucket Capacity: 10 tokens (maximum burst size)
  - Refill Rate: 10 tokens per minute (steady-state rate)
- **Response**: When rate limit is exceeded, returns HTTP 429 (Too Many Requests) with Retry-After header indicating seconds until next token availability

## Related Decisions
- ADR 002: Implement Outbox Pattern for Reliable Event Publishing
- ADR 003: Use Mock Notification Providers for Safety

## Notes
The Token Bucket implementation is located in:
- `src/main/java/com/example/demo/ratelimiter/TokenBucketService.java`
- `src/main/java/com/example/demo/ratelimiter/RateLimitFilter.java`
- Redis Lua scripts embedded in the TokenBucketService