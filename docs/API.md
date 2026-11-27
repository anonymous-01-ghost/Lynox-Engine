# Public API

This document describes the initial API surface. It is not yet a stable 1.0 contract.

## Entry point

`LynoxEngine` receives provider implementations through its constructor.

## Official YouTube Data API provider

The module includes `dev.lynox.engine.provider.YouTubeDataApiProvider`, which implements both `SearchProvider` and `MetadataProvider` using the official YouTube Data API v3.

Example on JVM desktop:

```kotlin
import dev.lynox.engine.LynoxEngine
import dev.lynox.engine.provider.YouTubeDataApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

val httpClient = HttpClient(CIO)
val youtube = YouTubeDataApiProvider(
    httpClient = httpClient,
    apiKey = System.getenv("YOUTUBE_API_KEY")
)
val engine = LynoxEngine(
    searchProvider = youtube,
    metadataProvider = youtube
)

// Close httpClient when the owning application shuts down.
```

Use the matching Ktor engine on Android (`Android`) and iOS (`Darwin`). The application owns the client lifecycle.

## Search

`SearchRequest` contains:
- `query`: required non-blank search text;
- `pageToken`: optional continuation token;
- `pageSize`: 1–50, default 20.

The YouTube adapter uses `search.list` with `type=video`. It maps video ID, title, description, channel, thumbnail and continuation token.

## Metadata

`MetadataProvider.getMediaInfo(mediaId)` uses `videos.list` to retrieve public metadata and ISO-8601 duration.

## Results and errors

Operations return `EngineResult.Success` or `EngineResult.Failure`. Expected operational errors are represented by `EngineError`; callers should not parse provider-specific exception messages.

## Scope

The adapter supports search and public metadata only. YouTube Data API does not provide raw playback streams. Playback must be implemented through supported YouTube playback integrations and applicable YouTube policies.

## Stability

Names and signatures may change before version 1.0. New public APIs must include KDoc and tests.
