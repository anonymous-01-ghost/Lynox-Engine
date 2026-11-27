# Lynox Engine

**Lynox Engine is an independent Kotlin Multiplatform media-extraction library project** for Android, iOS and JVM desktop. The intended integration is in-process: consuming applications add the library and call its API. A Lynox-hosted server, Google Cloud project, YouTube Data API key, or AI token is not a core requirement.

> **Status: extraction foundation in development.** The current repository previously grew around YouTube Data API search and metadata. That implementation is not the target extractor and must not be represented as one. The new source-resolver contract is a foundation only; a production-ready source adapter and real-device playback validation are still required.

## Product objective

Build Lynox-owned extraction infrastructure that LynoxMusic and other applications can consume as a dependency, without requiring NewPipe Extractor or another third-party extractor at runtime.

Targets:
- Android — Kotlin / AAR.
- iOS — Kotlin/Native XCFramework, callable from Swift through a stable wrapper.
- PC — Kotlin/JVM JAR.

## Architecture

- **Core:** provider-neutral models, typed results, validation and lifecycle.
- **Extractor contracts:** source URL to normalized media metadata and available renditions.
- **Source adapters:** separate implementations per source, isolated from the core.
- **Platform integration:** platform HTTP clients and playback hand-off; no mandatory hosted backend.

The new MediaResolver contract and ResolvedMedia / MediaStream models define the boundary. They do not themselves extract media. Source adapters must be implemented and tested independently.

## Current repository contents

- Kotlin Multiplatform targets for Android, iOS and JVM.
- Provider-neutral search, metadata and catalog contracts.
- In-memory cache for metadata/catalog responses.
- Retry and typed-error infrastructure.
- Legacy optional YouTube Data API adapters for search and public metadata. They are not the extraction engine, do not return media streams, and require a Google API key only if an application explicitly chooses to use them.
- GitHub Actions for JVM/Android and iOS builds; actual status is shown in GitHub Actions.

## Important technical boundary

The YouTube Data API returns search results and metadata, not raw audio/video streams. It cannot replace an extractor. Lynox Engine's core must not depend on Google Cloud quotas or API keys.

Source adapters must use access methods permitted for the source. The project will not implement DRM circumvention, access-control bypass, private credential use, or evasion of service restrictions. For YouTube playback, use supported playback integrations. The generic resolver contract can serve sources for which direct media resolution is authorized.

## Build

Requirements: JDK 17, Gradle 8.14.3, Android SDK for Android tasks, and macOS/Xcode for iOS tasks. A Gradle Wrapper is not yet committed.

    gradle :lynox-engine:jvmTest
    gradle :lynox-engine:compileKotlinJvm
    gradle :lynox-engine:compileDebugKotlinAndroid

On macOS, build the iOS XCFramework:

    gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework

## Documentation

- Architecture: docs/ARCHITECTURE.md
- Public API: docs/API.md
- Integration: docs/INTEGRATION.md
- Platform support: docs/PLATFORM_SUPPORT.md
- Compliance boundary: docs/COMPLIANCE.md
- License decision: LICENSE_SELECTION.md

The project is pre-1.0. Do not treat a queued CI run or a configured Gradle task as a successful build.
