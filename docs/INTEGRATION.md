# Consuming Lynox Engine

Lynox Engine is intended to be consumed as an in-process Kotlin Multiplatform library. The consuming application calls the library directly; no Lynox-hosted server, Google Cloud project, YouTube Data API key, or AI token is required by the core.

> **Important status:** the repository is currently an extraction foundation, not a finished replacement for NewPipe Extractor. The source-neutral resolver contract exists, but a production source adapter must still be implemented and validated.

## 1. Publish to a local Maven repository

From the Lynox Engine checkout, with JDK 17, Gradle 8.14.3 and Android SDK installed:

    gradle :lynox-engine:publishAllPublicationsToBuildDirectoryRepository -PVERSION_NAME=0.1.0-SNAPSHOT

Generated repository: lynox-engine/build/repository/.

Coordinates:
- Group: dev.lynox
- Module: lynox-engine
- Version: 0.1.0-SNAPSHOT unless overridden.

## 2. Add the dependency

Add the generated Maven repository to the consuming project's dependency repositories, then:

    dependencies {
        implementation("dev.lynox:lynox-engine:0.1.0-SNAPSHOT")
    }

Gradle Module Metadata selects the Android or JVM variant. iOS consumes the generated XCFramework.

## 3. Resolver integration boundary

A source adapter implements MediaResolver and returns ResolvedMedia containing available MediaStream renditions. The application injects the adapter into the engine integration layer and hands a selected, authorized rendition to its platform playback component.

The contract is not a working source adapter. Do not use a fake or placeholder resolver in production.

## 4. Platform artifacts

- Android: AAR.
- JVM desktop: JAR.
- iOS: static LynoxEngine.xcframework with device and simulator slices.

Build the iOS bundle on macOS with Xcode:

    gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework

Expected output: lynox-engine/build/XCFrameworks/release/LynoxEngine.xcframework.

## 5. Optional legacy YouTube metadata adapter

The repository still contains optional YouTube Data API adapters for search and public metadata. They are unrelated to media extraction and require a restricted API key only if explicitly configured. Do not add them when the application needs only the source-neutral extraction core.

## 6. Verification

A successful local publication task and platform CI jobs are required before calling a release verified. A queued workflow is not a passing build. Check GitHub Actions.
