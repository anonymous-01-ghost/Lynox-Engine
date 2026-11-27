# Architecture

Lynox Engine is organized around a provider-neutral public core and replaceable provider adapters.

## Dependency direction

```text
Consumer application
        |
Public Lynox API
        |
Domain models and contracts
        |
Provider adapter interfaces
        |
Approved provider implementation
```

The domain layer must not depend on undocumented YouTube endpoints or platform UI frameworks. Provider adapters translate provider responses into stable Lynox models. Platform-specific playback remains in platform modules and must use supported playback mechanisms.

## Source sets

- `commonMain`: API contracts, models, validation and shared logic.
- `androidMain`: Android-specific integrations.
- `iosMain`: Kotlin/Native iOS integrations.
- `jvmMain`: desktop/JVM integrations.
- `commonTest`: platform-neutral tests.

Keep platform-specific code behind small interfaces. Introduce `expect/actual` only where dependency injection cannot provide a simpler design.
