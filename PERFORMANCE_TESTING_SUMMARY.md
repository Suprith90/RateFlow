# Performance Testing Setup for RateFlow Project

I have created a comprehensive performance testing setup for the RateFlow application using k6 as requested in the task.

## What Was Created

### 1. Main Test Script
- `performance-test/rateflow-performance-test.js`: Main k6 script containing all test functions
  - Rate-limited API testing (`/api/resource`)
  - Notification creation (`POST /api/v1/notifications`)
  - Notification retrieval (`GET /api/v1/notifications/{id}`)
  - Notification listing (`GET /api/v1/notifications`)

### 2. Supporting Files
- `performance-test/k6-test-script.js`: Shared helper functions and test data
- `performance-test/scenarios.js`: Seven different test scenarios as requested:
  1. Traffic below the rate limit
  2. Traffic above the rate limit
  3. Multiple authenticated users
  4. Short traffic bursts
  5. Sustained traffic
  6. Notification creation under load
  7. Notification processing while consumers fall behind
- `performance-test/k6-options.js`: Thresholds and configuration for pass/fail criteria
- `performance-test/README.md`: Detailed instructions on how to run the tests
- `performance-test/run_all_scenarios.sh`: Script to run all scenarios sequentially
- `performance-test/validate_setup.sh`: Script to validate application readiness
- `performance-test/PERFORMANCE_REPORT_TEMPLATE.md`: Template for reporting results

### 3. Test Scenarios Implemented

#### Scenario 1: Traffic Below the Rate Limit
- 5 VUs for 2 minutes
- Tests normal operation within rate limits
- Validates system behavior under typical load

#### Scenario 2: Traffic Above the Rate Limit
- 50 VUs for 3 minutes
- Tests rate limiting effectiveness
- Verifies appropriate 429 responses and backoff headers

#### Scenario 3: Multiple Authenticated Users
- 20 VUs for 7 minutes
- Tests concurrent user handling
- Validates user isolation and data integrity

#### Scenario 4: Short Traffic Bursts
- Ramp-up to 100 VUs over 30s, sustain 10s, ramp-down over 30s
- Tests system responsiveness to sudden load spikes
- Measures recovery time and stability

#### Scenario 5: Sustained Traffic
- 25 VUs for 10 minutes
- Tests long-term stability
- Monitors for memory leaks and performance degradation

#### Scenario 6: Notification Creation Under Load
- 15 VUs for 8 minutes
- Focuses on notification creation throughput
- Measures database and service performance under load

#### Scenario 7: Notification Processing Lag
- 30 VUs for 15 minutes
- Creates backlog to observe consumer lag
- Tests system behavior when processors fall behind

## Key Features of the Implementation

### Realistic Test Data
- Dynamically generated unique identifiers to prevent collisions
- Realistic notification content with JSON payloads
- Properly formatted email addresses and phone numbers
- Unique idempotency keys to test deduplication

### Comprehensive Metrics Collection
- Custom k6 metrics for:
  - Rate limiting (429 rates, decision latency)
  - Notification operations (creation, retrieval, listing latency)
  - Error rates and success rates
- Standard HTTP metrics (response times, failure rates)
- Application-level metrics that can be correlated with actuator data

### Proper Test Design
- Think time between requests to simulate realistic user behavior
- Ramp-up/ramp-down patterns to avoid shocking the system
- Mixed workload scenarios for realistic usage patterns
- Clear success/failure criteria with configurable thresholds

### Safety and Isolation
- Uses test data that won't interfere with production systems
- Notification providers are mocked in the application (no real emails/SMS sent)
- Test data is ephemeral and unique to each test run
- Clear separation between test and production concerns

## How to Use

1. **Validate Setup**: Run `./performance-test/validate_setup.sh` to check application readiness
2. **Run Individual Tests**: Use `k6 run` with appropriate parameters or scenarios
3. **Run Full Suite**: Use `./performance-test/run_all_scenarios.sh` to execute all tests
4. **Generate Reports**: Fill in `PERFORMANCE_REPORT_TEMPLATE.md` with measured values

## Example Commands

```bash
# Validate the setup
./performance-test/validate_setup.sh

# Run a quick smoke test
k6 run --vus 5 --duration 2m performance-test/rateflow-performance-test.js

# Run the above rate limit scenario
k6 run --vus 50 --duration 3m performance-test/rateflow-performance-test.js

# Run all scenarios sequentially
./performance-test/run_all_scenarios.sh
```

## Integration with Observability Features

The performance tests are designed to work with the observability features implemented previously:
- Custom metrics can be correlated with Spring Boot Actuator metrics
- Rate limiting decisions are tracked via custom metrics
- Notification processing times are measured
- Outback backlog and retry metrics can be validated
- Kafka consumer lag can be monitored during test execution

## Notes on Execution

- Ensure the RateFlow application is running before executing tests
- Make sure required services (PostgreSQL, Redis, Kafka) are available
- The tests assume security is either disabled for testing or can be bypassed via headers
- For production-like testing, consider creating proper test users and authentication flows
- Monitor system resources during test execution to identify bottlenecks
- Consider running tests in a staging environment that mirrors production

This performance testing setup provides a comprehensive framework for validating the RateFlow application's behavior under various load conditions, identifying bottlenecks, and ensuring the system meets performance requirements before deployment to production.