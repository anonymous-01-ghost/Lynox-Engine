# Lynox Engine

Independent Kotlin Multiplatform library targeting **Android, iOS and JVM desktop**.

> Current status: early development. The shared engine, official YouTube Data API adapter, in-memory cache and multiplatform CI are implemented. A stable 1.0 release is not yet available.

## Modules and targets

- `:lynox-engine` — shared SDK module.
- Android — Android library (AAR).
- iOS — device and simulator frameworks (including Apple Silicon and Intel simulator targets).
- Desktop — JVM library (JAR), usable on JVM-based desktop environments.

## Features implemented

- Provider-neutral engine API and typed results.
- Search pagination models.
- Provider-neutral catalog contracts and YouTube catalog adapter for channel search, playlist search, channel/playlist metadata, and paginated playlist entries; see [docs/CATALOG_API.md](docs/CATALOG_API.md).
- Official YouTube Data API v3 adapter:
  - video search through `search.list`;
  - public metadata and duration through `videos.list`.
- YouTube catalog adapter using `search.list`, `channels.list`, `playlists.list` and `playlistItems.list`.
- Ktor networking with Android, Darwin and CIO engines.
- Bounded HTTP retries (up to 3 attempts) for transient `429` and `5xx` responses, honoring numeric `Retry-After` values within a capped delay. Authentication, quota (`403`), validation (`400`) and other client errors are not retried.
- Configurable, bounded, in-memory LRU cache with TTL for successful video search, metadata, channel/playlist searches, channel/playlist metadata, and playlist-entry pages.
- Cache invalidation for all cached response categories through `engine.clearCache()`. Failed provider responses are never cached; cache entries are process-local and expire according to the configured TTL.
- Shared unit tests and GitHub Actions builds for Android, iOS and JVM.

## Basic integration

Create a platform-specific Ktor client and inject the official provider:

```kotlin
val provider = YouTubeDataApiProvider(
    httpClient = httpClient,
    apiKey = restrictedYouTubeApiKey
)

val engine = LynoxEngine(
    searchProvider = provider,
    metadataProvider = provider,
    config = LynoxConfig(
        cacheTtl = 10.minutes,
        maxCacheEntries = 100
    )
)

val result = engine.search(SearchRequest(query = "ambient music"))
```

Import `kotlin.time.Duration.Companion.minutes` when using the example TTL.

## Build

Requirements: JDK 17, Gradle 8.14.3, Android SDK for Android tasks, and macOS with Xcode for iOS tasks.

The Gradle Wrapper has not yet been committed. Until it is added, use a compatible local Gradle installation:

```bash
gradle :lynox-engine:jvmTest
gradle :lynox-engine:compileKotlinJvm
gradle :lynox-engine:compileDebugKotlinAndroid
```

iOS framework compilation requires macOS and Xcode.

## Integration notes

The consuming app creates the Ktor `HttpClient` using its platform engine and injects it into `YouTubeDataApiProvider` and/or `YouTubeDataApiCatalogProvider`. Pass the catalog adapter as `catalogProvider` to `LynoxEngine` to enable channel and playlist operations. The app owns the client lifecycle and supplies its YouTube Data API key.

Do not commit API keys. Restrict keys in Google Cloud and monitor API quota. A key embedded in a distributed client cannot be treated as a secret; apply API restrictions and evaluate a trusted backend if stronger credential protection is required.

## Playback and compliance boundary

The YouTube Data API provides search and metadata; it does not provide raw media streams. This library does not scrape private endpoints, extract streams, bypass ads/DRM, or implement unauthorized playback. Use supported YouTube playback integrations and follow YouTube API Services terms and policies.

## License

The project license has not yet been selected. See [LICENSE_SELECTION.md](LICENSE_SELECTION.md) before external distribution.
