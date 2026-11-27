# Lynox YouTube Engine

Independent Kotlin Multiplatform library project targeting **Android, iOS and JVM desktop**.

> Current status: foundation / scaffold. The public contracts and initial models are in place. Provider integrations, platform validation and release artifacts are not yet complete.

## Repository layout

- `:lynox-engine` — shared SDK module.
- `commonMain` — platform-neutral API and models.
- `androidMain`, `iosMain`, `jvmMain` — platform-specific extension points.
- `commonTest` — shared tests.
- `PLAN.md` — master development plan.
- `docs/` — architecture, API, platform and compliance documentation.

## Targets

- Android library (AAR)
- iOS device and simulator frameworks
- JVM desktop library (JAR)

## Build

Requirements: JDK 17 or the version required by the selected Android Gradle Plugin, Android SDK for Android tasks, and macOS with Xcode for iOS tasks.

The Gradle Wrapper should be generated and committed from a trusted local Gradle installation before the first reproducible release build.

Typical tasks after wrapper setup:

```bash
./gradlew :lynox-engine:tasks
./gradlew :lynox-engine:compileKotlinMetadata
./gradlew :lynox-engine:compileKotlinJvm
./gradlew :lynox-engine:compileDebugKotlinAndroid
```

iOS framework compilation requires macOS and Xcode.

## Current implementation

- Public engine entry point
- Search and metadata provider contracts
- Provider-neutral media and pagination models
- Typed result/error model
- Basic configuration model
- Initial common tests

No YouTube provider is wired in yet. This is intentional: provider access must use documented and permitted interfaces, and must not rely on private endpoints or bypass mechanisms.

## License

The project license has not yet been selected. See [LICENSE_SELECTION.md](LICENSE_SELECTION.md) before external distribution.
