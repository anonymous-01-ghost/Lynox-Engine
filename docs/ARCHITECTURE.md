# Architecture

Lynox Engine is an in-process Kotlin Multiplatform library. The core owns normalized models and public contracts; source-specific adapters are separate implementations injected into the engine.

## Dependency direction

    Consuming application (LynoxMusic, Android/iOS/PC apps)
                         |
                  Lynox public API
                         |
             Core models + typed results
                         |
                MediaResolver contract
                         |
             Source-specific adapter module
                         |
          Permitted source access mechanisms

The core must not require a Lynox backend, Google Cloud project, YouTube Data API key, or AI service. Applications own their platform lifecycle and playback UI.

## Extraction contract

- MediaResolver.resolve(sourceUrl) resolves a source URL.
- ResolvedMedia represents the resolved media item.
- MediaStream represents an available audio, video, or combined rendition.
- Stream URLs may expire. They are not placed in the existing long-lived metadata cache.
- Adapters own source-specific parsing, network behavior, refresh rules and capability reporting.
- An adapter must use access methods permitted for its source; it must not bypass DRM, authentication, paywalls or other access controls.

The contract is not an extractor implementation. A source adapter is only complete after fixture-based parser tests, network/error tests, and playback validation on each claimed platform.

## Source sets

- commonMain: API contracts, models, validation and shared logic.
- androidMain: Android-specific integrations.
- iosMain: Kotlin/Native iOS integrations.
- jvmMain: desktop/JVM integrations.
- commonTest: platform-neutral tests.

Keep source-specific code behind interfaces. Avoid embedding source-specific HTTP assumptions in the core.
