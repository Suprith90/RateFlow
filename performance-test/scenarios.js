// Different test scenarios for the RateFlow application

export const scenarios = {
  // Scenario 1: Traffic below the rate limit
  below_rate_limit: {
    executor: 'constant-vus',
    exec: 'testRateLimitedAPI',
    vus: 5, // 5 virtual users
    duration: '2m', // 2 minutes
    startTime: '0s',
  },

  // Scenario 2: Traffic above the rate limit
  above_rate_limit: {
    executor: 'constant-vus',
    exec: 'testRateLimitedAPI',
    vus: 50, // 50 virtual users to exceed rate limit
    duration: '3m', // 3 minutes
    startTime: '0s',
  },

  // Scenario 3: Multiple authenticated users
  multiple_users: {
    executor: 'constant-vus',
    exec: 'testNotificationCreation',
    vus: 20, // 20 different users
    duration: '5m', // 5 minutes
    startTime: '0s',
  },

  // Scenario 4: Short traffic bursts
  short_bursts: {
    executor: 'ramping-vus',
    exec: 'testRateLimitedAPI',
    startVUs: 0,
    stages: [
      { duration: '30s', target: 100 }, // Ramp up to 100 VUs in 30s
      { duration: '10s', target: 100 }, // Stay at 100 VUs for 10s
      { duration: '30s', target: 0 },   // Ramp down to 0 in 30s
    ],
    gracefulRampDown: '0s',
  },

  // Scenario 5: Sustained traffic
  sustained_traffic: {
    executor: 'constant-vus',
    exec: 'testRateLimitedAPI',
    vus: 25, // 25 VUs sustained
    duration: '10m', // 10 minutes
    startTime: '0s',
  },

  // Scenario 6: Notification creation under load
  notification_creation_load: {
    executor: 'constant-vus',
    exec: 'testNotificationCreation',
    vus: 15, // 15 VUs creating notifications
    duration: '7m', // 7 minutes
    startTime: '0s',
  },

  // Scenario 7: Notification processing while consumers fall behind
  notification_processing_lag: {
    executor: 'constant-vus',
    exec: 'testNotificationCreation',
    vus: 30, // High load to create backlog
    duration: '12m', // Long duration to observe lag
    startTime: '0s',
  },
};