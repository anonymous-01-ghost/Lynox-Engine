# Testing strategy

## Common tests
Test validation, model mapping, pagination, error conversion and cache policy without network access.

## Provider contract tests
Use recorded, sanitized fixtures and mock transports. Fixtures must not contain personal data, credentials or restricted content.

## Platform tests
Run Android and JVM checks on Linux CI. Run iOS compilation and tests on macOS with Xcode.

## Live integration checks
Keep live provider checks opt-in and isolated from pull-request CI. Use dedicated development credentials stored as CI secrets and enforce low request volumes.
