# Networking strategy

The shared SDK should depend on abstractions, not a specific provider protocol.

Planned responsibilities:
- HTTP client configuration and lifecycle;
- request timeout and cancellation;
- bounded retry policy for transient failures only;
- response parsing and validation;
- quota and rate-limit handling;
- structured errors that do not leak secrets.

A concrete HTTP client dependency will be selected after validating supported Kotlin Multiplatform targets. Do not retry authentication failures, invalid requests or quota failures blindly. Respect provider retry instructions and rate limits.
