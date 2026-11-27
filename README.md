# Lynox Engine

Lynox Engine is an independent Kotlin Multiplatform media-extraction library project for Android, iOS and JVM desktop. Applications integrate it directly as a dependency. It does not require a Lynox-hosted server, Google Cloud project, YouTube Data API key, or AI token.

> **Status: extraction foundation in development.** The repository was previously misdirected toward YouTube Data API search and metadata. Those adapters have been removed from the core. The source-resolver contract is now integrated, but a production source adapter and real-device playback validation are still pending.

## Product objective

Build Lynox-owned extraction infrastructure for LynoxMusic and other applications, without requiring NewPipe Extractor or another third-party extractor at runtime.

Targets:
- Android — Kotlin / AAR.
- iOS — Kotlin/Native XCFramework, callable from Swift through a stable wrapper.
- PC — Kotlin/JVM JAR.

## Architecture

- **Core:** provider-neutral models, typed results, validation and lifecycle.
- **Extractor contract:** source URL to normalized media metadata and available renditions.
- **Source adapters:** separate implementations per source, isolated from the core.
- **Platform integration:** platform HTTP clients and playback hand-off; no mandatory hosted backend.

The core now exposes MediaResolver through LynoxEngine.resolveMedia(). It defines the integration contract, but does not itself implement source-specific extraction. A real source adapter is still required.

## Current repository contents

- Kotlin Multiplatform targets for Android, iOS and JVM.
- Provider-neutral search, metadata and catalog contracts.
- MediaResolver, ResolvedMedia, MediaStream and MediaStreamKind.
- URL validation and tests for the resolver entry point.
- In-memory cache for metadata/catalog responses.
- Typed engine results and errors with a transport-neutral core.

## Technical boundary

The YouTube Data API returns search results and metadata, not raw audio/video streams, so it is not an extractor. Its adapter code has been removed from this core project. The core has no Google Cloud API-key or quota requirement.

Every source adapter must use access methods permitted for that source. The project will not implement DRM circumvention, access-control bypass, private credential use, or evasion of service restrictions. For YouTube playback, use supported playback integrations. The generic resolver contract does not itself authorize direct extraction of YouTube media.

## Build

Requirements: JDK 17, Gradle 8.14.3, Android SDK for Android tasks, and macOS/Xcode for iOS tasks. A Gradle Wrapper is not yet committed.

    gradle :lynox-engine:jvmTest
    gradle :lynox-engine:compileKotlinJvm
    gradle :lynox-engine:compileDebugKotlinAndroid

On macOS:

    gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework

## Documentation

- Architecture: docs/ARCHITECTURE.md
- Public API: docs/API.md
- Integration: docs/INTEGRATION.md
- LynoxMusic migration target: docs/LYNOXMUSIC_MIGRATION.md
- Platform support: docs/PLATFORM_SUPPORT.md
- Compliance: docs/COMPLIANCE.md
- License decision: LICENSE_SELECTION.md

Do not treat a queued CI run or a configured Gradle task as a successful build.
