# Consuming Lynox Engine

Lynox Engine is an in-process Kotlin Multiplatform library. The consuming application calls the library directly; no Lynox-hosted server, Google Cloud project, YouTube Data API key, or AI token is required.

> **Status:** the repository is an extraction foundation, not yet a finished replacement for NewPipe Extractor. A direct-file resolver is included, but service-specific search, metadata, and playback adapters remain unimplemented.

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

## 3. Resolve a direct media file

For a publicly accessible direct media-file URL with a recognized extension:

    val engine = LynoxEngine(mediaResolver = DirectUrlMediaResolver())
    val result = engine.resolveMedia("https://cdn.example.test/audio/song.m4a")

This adapter classifies the URL by extension and returns one rendition. It does not download, search, or parse service pages. A successful resolution only identifies a candidate URL; actual playback and server response must be verified by the platform player.

## 4. Implement and inject a source resolver

    val engine = LynoxEngine(mediaResolver = mySourceResolver)
    val result = engine.resolveMedia("https://source.example/media/item")

The application may supply another MediaResolver implementation for a source it is authorized to access. The returned rendition is passed to the platform playback component.

This is an extension point, not an end-to-end source adapter. Do not use a fake or placeholder resolver in production.

## 5. Resolve an HLS master playlist with the native transport

The library provides a platform factory so the consuming app does not need to
construct the HTTP client manually. Import
`dev.lynox.engine.extractor.createPlatformHlsManifestResolver` in the
platform source set:

    val engine = LynoxEngine(
        mediaResolver = createPlatformHlsManifestResolver()
    )
    val result = engine.resolveMedia("https://cdn.example.test/master.m3u8")

The factory selects:
- Android: `AndroidManifestHttpClient` backed by `HttpURLConnection`.
- JVM desktop: `JvmManifestHttpClient` backed by `HttpURLConnection`.
- iOS: `IosManifestHttpClient` backed by `URLSession`.

The resolver fetches and parses an HLS master playlist. It does not implement
service-specific search, metadata extraction, protected-stream access, DRM
handling, or media-segment downloading. The iOS URLSession completion-handler
transport currently buffers the response before checking its configured byte
limit; strict pre-buffer memory limiting still requires a delegate-based
streaming implementation. The consuming app remains responsible for its
platform network policy (Android cleartext configuration and iOS App Transport
Security).

## 6. Platform artifacts

- Android: AAR.
- JVM desktop: JAR.
- iOS: static LynoxEngine.xcframework with device and simulator slices.

Build the iOS bundle on macOS with Xcode:

    gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework

Expected output: lynox-engine/build/XCFrameworks/release/LynoxEngine.xcframework.

## 7. Verification

A successful local publication task and platform CI jobs are required before calling a release verified. A queued workflow is not a passing build. Check GitHub Actions.
