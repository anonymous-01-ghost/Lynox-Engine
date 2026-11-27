# Platform HTTP client setup

Lynox Engine is a client-side Kotlin Multiplatform library. The core does not require a Lynox server, Google Cloud project, YouTube Data API key, or AI service. A source adapter may use Ktor when its source protocol requires HTTP; the consuming application owns the client lifecycle.

## Platform engines

| Target | Ktor engine |
| --- | --- |
| Android | ktor-client-android |
| iOS | ktor-client-darwin |
| JVM desktop | ktor-client-cio |

Create one application-scoped client rather than a new client for every resolution request. Configure timeouts appropriate to the source and close the client when its owner is disposed.

## Android

    val client = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

## iOS

    val client = HttpClient(Darwin) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

## JVM desktop

    val client = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

Add the matching Ktor engine dependency to the consuming app if it creates the client. Keep versions aligned with the app and close the client exactly once.

## Source adapter responsibilities

A MediaResolver implementation may receive an injected HttpClient, but the generic contract does not mandate Ktor or a particular network strategy. The adapter is responsible for source-specific redirects, headers, parsing, retries, expiration and refresh behavior. Never log authorization headers, cookies, tokens or signed URLs.


## Playback capability discovery

The common module exposes `MediaPlaybackCapabilityProvider` and a static implementation for known profiles and tests. Automatic Media3 renderer and desktop-player integrations are not included yet. Android codec discovery and an iOS AVFoundation format provider are described below. Each consuming application must currently provide a profile based on its actual player; do not infer support solely from the platform name. A future platform adapter must query the playback stack and be tested on that platform before it is described as an automatic capability detector.

### Android decoder capability provider

The Android source set includes `AndroidMediaCodecCapabilityProvider`. It queries regular Android decoder entries through `MediaCodecList` and returns supported MIME types, audio/video availability and aggregate upper bounds exposed by codec capabilities.

```kotlin
val capabilityProvider = AndroidMediaCodecCapabilityProvider()
val capabilities = capabilityProvider.getCapabilities()
```

The returned dimensions and bitrate limits are aggregate device-level ceilings, not a promise that one decoder supports every combination of those maxima. The provider does not report codec profile/level identifiers, does not inspect Media3 renderer configuration, and does not resolve or download media. Applications should still validate playback with their configured player. iOS/AVFoundation and desktop-player providers remain unimplemented.


### iOS AVFoundation format provider

The iOS source set includes `IosAvFoundationCapabilityProvider`. It reads the MIME types recognized by `AVURLAsset` and exposes the audio/video categories to the common selector.

```kotlin
val capabilityProvider = IosAvFoundationCapabilityProvider()
val capabilities = capabilityProvider.getCapabilities()
```

This is framework-level container/MIME recognition, not a per-device codec-profile query or a guarantee that a particular AVPlayer item can play every stream. It does not inspect an active AVPlayer configuration, resolve media URLs, or download content. Device validation remains required.
