# Consuming Lynox Engine

Lynox Engine is an in-process Kotlin Multiplatform library. The consuming application calls the library directly; no Lynox-hosted server, Google Cloud project, YouTube Data API key, or AI token is required.

> **Status:** the repository is an extraction foundation, not yet a finished replacement for NewPipe Extractor. The public resolver contract is implemented, but a production source adapter must still be implemented and validated.

## 1. Publish to a local Maven repository

    gradle :lynox-engine:publishAllPublicationsToBuildDirectoryRepository -PVERSION_NAME=0.1.0-SNAPSHOT

Generated repository: lynox-engine/build/repository/.

Coordinates:
- Group: dev.lynox
- Module: lynox-engine
- Version: 0.1.0-SNAPSHOT unless overridden.

## 2. Add the dependency

Add the generated Maven repository to the consuming project's dependency repositories:

    dependencies {
        implementation("dev.lynox:lynox-engine:0.1.0-SNAPSHOT")
    }

Gradle Module Metadata selects the Android or JVM variant. iOS consumes the generated XCFramework.

## 3. Implement and inject a source resolver

    val engine = LynoxEngine(mediaResolver = mySourceResolver)
    val result = engine.resolveMedia("https://source.example/media/item")

The application supplies mySourceResolver, an implementation of MediaResolver. It must resolve only sources and media that the application is authorized to access. The returned rendition is passed to the platform playback component.

This is an integration example, not an end-to-end source adapter. Do not use a fake or placeholder resolver in production.

## 4. Platform artifacts

- Android: AAR.
- JVM desktop: JAR.
- iOS: static LynoxEngine.xcframework with device and simulator slices.

Build the iOS bundle on macOS with Xcode:

    gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework

Expected output: lynox-engine/build/XCFrameworks/release/LynoxEngine.xcframework.

## 5. Verification

A successful local publication task and platform CI jobs are required before calling a release verified. A queued workflow is not a passing build. Check GitHub Actions.
