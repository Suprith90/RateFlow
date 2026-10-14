# ADR 003: Use Mock Notification Providers for Safety

## Status
Accepted

## Date
2026-10-14

## Context
We need to test notification functionality including creation, processing, and delivery without sending actual emails or SMS messages, which could incur costs, cause spam complaints, or violate privacy regulations during development and testing.

## Decision
We created mock implementations of the email and SMS notification providers that simulate the behavior of real providers without actually sending messages.

## Consequences

### Pros
- **Safe Testing**: Enables comprehensive testing of notification flows without risk of sending unintended messages
- **Cost Elimination**: Avoids charges from third-party messaging services during development and testing
- **Privacy Protection**: Prevents accidental use of real customer contact information in test environments
- **Deterministic Behavior**: Mock providers provide consistent, predictable responses for testing
- **Development Speed**: Eliminates dependency on external service availability or rate limits during development

### Cons
- **Production Incompatibility**: Mock providers are not suitable for production use without replacement
- **Feature Gap**: Does not test actual delivery mechanisms, bounce handling, or provider-specific error responses
- **Integration Testing Gap**: Requires separate integration testing with real providers before production deployment

### Implementation Details
- **MockEmailProvider**: 
  - Logs email details (recipient, subject, body) at INFO level instead of sending
  - Returns success response immediately
  - Can be configured to simulate failures for testing error handling
- **MockSMSProvider**: 
  - Logs SMS details (recipient, message) at INFO level instead of sending
  - Returns success response immediately
  - Can be configured to simulate failures for testing error handling
- **Configuration**: 
  - Providers are wired as Spring beans
  - Easy to replace with real provider implementations by changing the bean configuration
  - No changes required to notification service or consumer logic

## Related Decisions
- ADR 001: Use Token Bucket for Rate Limiting
- ADR 002: Implement Outbox Pattern for Reliable Event Publishing

## Notes
The mock notification providers are located in:
- `src/main/java/com/example/demo/notification/provider/MockEmailProvider.java`
- `src/main/java/com/example/demo/notification/provider/MockSMSProvider.java`

To replace with real providers in production:
1. Implement EmailProvider and SmsProvider interfaces using actual service APIs (e.g., AWS SES, SendGrid, Twilio)
2. Configure appropriate credentials via environment variables or secrets management
3. Replace the bean definitions in the configuration classes
4. Update error handling to accommodate real provider failure modes