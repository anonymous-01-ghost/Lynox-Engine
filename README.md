# Lynox Engine

Independent Kotlin Multiplatform library targeting **Android, iOS and JVM desktop**.

> Current status: early development. Shared contracts and the first official YouTube Data API adapter are in place. Platform builds and integration tests still require CI verification.

## Modules and targets

- `:lynox-engine` — shared SDK module.
- Android — Android library (AAR).
- iOS — device and simulator frameworks (including Apple Silicon and Intel simulator targets).
- Desktop — JVM library (JAR), usable on JVM-based desktop environments.

## Features implemented

- Provider-neutral engine API and typed results.
- Search pagination models.
- Official YouTube Data API v3 adapter:
  - video search through `search.list`;
  - public metadata and duration through `videos.list`.
- Ktor networking with Android, Darwin and CIO engines.
- Shared model tests and GitHub Actions build workflow.

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

The consuming app creates the Ktor `HttpClient` using its platform engine and injects it into `YouTubeDataApiProvider`. The app owns the client lifecycle and supplies its YouTube Data API key.

Do not commit API keys. Restrict keys in Google Cloud and monitor API quota. For distributed apps, evaluate key protection and whether a trusted backend is necessary.

## Playback and compliance boundary

The YouTube Data API provides search and metadata; it does not provide raw media streams. This library does not scrape private endpoints, extract streams, bypass ads/DRM, or implement unauthorized playback. Use supported YouTube playback integrations and follow YouTube API Services terms and policies.

## License

The project license has not yet been selected. See [LICENSE_SELECTION.md](LICENSE_SELECTION.md) before external distribution.
