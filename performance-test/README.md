# RateFlow Performance Testing Setup

This directory contains performance tests for the RateFlow application using k6.

## Prerequisites

1. [k6](https://k6.io/) installed (version 0.45.0 or later)
2. RateFlow application running and accessible
3. Required services (PostgreSQL, Redis, Kafka) running
4. Test data prepared (if needed)

## Installation

### Install k6

#### On macOS:
```bash
brew install k6
```

#### On Ubuntu/Debian:
```bash
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 379CE192D401AB61
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

#### On Windows:
Using Chocolatey:
```bash
choco install k6
```

Or download from [k6.io/downloads](https://k6.io/downloads/)

## Test Files Overview

- `rateflow-performance-test.js`: Main test script with all scenarios
- `k6-test-script.js`: Shared test functions
- `scenarios.js`: Different test scenario configurations
- `k6-options.js`: Thresholds and options configuration
- `PERFORMANCE_REPORT_TEMPLATE.md`: Template for reporting results
- `README.md`: This file

## Running Tests

### Quick Test (Smoke Test)
```bash
k6 run --vus 5 --duration 30s rateflow-performance-test.js
```

### Run Specific Scenarios
To run a specific scenario, you can modify the test script or use environment variables:

```bash
# Test below rate limit
K6_SCENARIO=below_rate_limit k6 run rateflow-performance-test.js

# Test above rate limit  
K6_SCENARIO=above_rate_limit k6 run rateflow-performance-test.js

# Test notification creation under load
K6_SCENARIO=notification_creation_load k6 run rateflow-performance-test.js
```

### Run All Tests Sequentially
```bash
# Below rate limit
k6 run -e SCENARIO=below_rate_limit rateflow-performance-test.js

# Above rate limit
k6 run -e SCENARIO=above_rate_limit rateflow-performance-test.js

# Multiple users
k6 run -e SCENARIO=multiple_users rateflow-performance-test.js

# Short bursts
k6 run -e SCENARIO=short_bursts rateflow-performance-test.js

# Sustained traffic
k6 run -e SCENARIO=sustained_traffic rateflow-performance-test.js

# Notification creation load
k6 run -e SCENARIO=notification_creation_load rateflow-performance-test.js

# Notification processing lag
k6 run -e SCENARIO=notification_processing_lag rateflow-performance-test.js
```

### Custom Configuration
You can override various parameters using environment variables:

```bash
# Change base URL
BASE_URL=http://staging.example.com:8080 k6 run rateflow-performance-test.js

# Change request timeout
REQUEST_TIMEOUT=10s k6 run rateflow-performance-test.js

# Change request interval (think time)
REQUEST_INTERVAL=0.5 k6 run rateflow-performance-test.js

# Increase duration for soak test
DURATION=30m k6 run --vus 25 --duration 30m rateflow-performance-test.js
```

### Output Results to File
```bash
# Save results in JSON format for further processing
k6 run --out json=results.json rateflow-performance-test.js

# Save results in CSV format
k6 run --out csv=results.csv rateflow-performance-test.js

# Send results to k6 Cloud (if you have an account)
k6 run --out cloud rateflow-performance-test.js
```

## Understanding the Tests

### API Endpoints Tested

1. **Rate Limited API**: `GET /api/resource`
   - Tests the rate limiting functionality
   - Uses `X-User-ID` header to identify users
   - Configured for 10 requests per minute per user in the controller

2. **Notification Creation**: `POST /api/v1/notifications`
   - Creates new notifications
   - Requires authentication (simulated via headers in tests)
   - Validates input and creates outbox events

3. **Get Notification**: `GET /api/v1/notifications/{id}`
   - Retrieves a specific notification by ID
   - Checks ownership and returns notification details

4. **List Notifications**: `GET /api/v1/notifications`
   - Lists notifications for the authenticated user
   - Supports pagination

### Rate Limiting Configuration
In `RateLimitController.java`, the rate limit is set to:
- 10 requests per minute per user
- Implemented using Token Bucket algorithm with Redis
- Returns HTTP 429 when limit is exceeded
- Includes Retry-After header in 429 responses

### Test Data
- Uses dynamically generated test data to avoid collisions
- Notification recipients are generated with timestamps to ensure uniqueness
- Idempotency keys are generated to prevent duplicate processing in real scenarios
- User IDs are drawn from a shared array to ensure distribution across virtual users

## Monitoring During Tests

While running tests, you should monitor:

### Application Metrics (via Actuator)
```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics (including custom ones)
curl http://localhost:8080/actuator/metrics

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

### System Resources
- CPU, Memory, Disk I/O on application server
- Database connections and query performance
- Redis memory usage and hit rate
- Kafka broker lag and throughput
- Network utilization

## Interpreting Results

### Key Metrics to Watch

1. **Response Latency**:
   - Look for increases over time (memory leaks)
   - Watch for spikes during garbage collection
   - Monitor 95th and 99th percentiles for tail latency

2. **Error Rates**:
   - HTTP 5xx indicate server problems
   - HTTP 429 indicate rate limiting working correctly
   - HTTP 4xx indicate client errors (validation, auth, etc.)

3. **Throughput**:
   - Requests per second sustained over time
   - Compare to expected capacity

4. **Resource Utilization**:
   - CPU: Should stay below 80% for headroom
   - Memory: Should not show continuous growth
   - Disk I/O: Should be within expected bounds
   - Network: Should not saturate interfaces

### Success Criteria
- Error rate < 1% (excluding expected 429s during overload tests)
- 95th percentile latency < 500ms for most endpoints
- System remains stable throughout test duration
- Resource utilization shows no concerning trends
- Rate limiting behaves as expected (correct percentage blocked)

## Troubleshooting

### Common Issues

1. **Connection Refused**:
   - Verify application is running: `curl http://localhost:8080/actuator/health`
   - Check BASE_URL environment variable
   - Verify network connectivity

2. **Authentication Errors**:
   - The tests use header-based authentication simulation
   - If real authentication is required, modify test scripts to include auth flow
   - Check if security configuration needs adjustment for testing

3. **Database Connection Errors**:
   - Verify PostgreSQL is running and accessible
   - Check connection pool settings in application
   - Verify database schema is migrated

4. **Redis Connection Errors**:
   - Verify Redis is running and accessible
   - Check Redis configuration in application
   - Verify Redis is properly initialized

5. **Kafka Connection Errors**:
   - Verify Kafka and Zookeeper are running
   - Check Kafka bootstrap servers configuration
   - Verify topics exist or can be auto-created

### Debugging Tips
- Run with verbose output: `k6 run --vus 1 --duration 10s http://debugk6.io/test.txt`
- Check application logs for errors
- Enable debug logging in application if needed
- Use k6's built-in debug mode: `K6_DEBUG=1 k6 run script.js`

## Best Practices for Performance Testing

1. **Isolate Test Environment**: Use a dedicated environment that mirrors production
2. **Warm Up Properly**: Allow system to reach steady state before measuring
3. **Run Long Enough**: Tests should run long enough to reveal memory leaks and other time-dependent issues
4. **Monitor Everything**: Application metrics, system metrics, and business metrics
5. **Test Realistic Patterns**: Use production-like data distributions and request mixes
6. **Isolate Variables**: Change only one thing at a time when troubleshooting
7. **Repeat Tests**: Run tests multiple times to ensure consistency
8. **Document Everything**: Keep detailed records of test configurations and results

## Safety Notes

- These tests generate load that could affect other services in shared environments
- Never run sustained load tests against production systems without explicit permission
- Monitor for unintended side effects (emails/SMS sent, etc.)
- The test scripts use safe test data and avoid real external service calls
- Notification providers are mocked in the application, so no real messages are sent