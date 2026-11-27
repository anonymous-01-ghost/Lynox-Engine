# Channel and playlist catalog

Lynox Engine is an in-process Kotlin Multiplatform **library**. It does not require a Lynox-hosted API or server.

## Public models and provider contract

The `dev.lynox.engine.catalog` package defines:

- `Channel`: normalized public channel metadata.
- `Playlist`: normalized public playlist metadata.
- `PlaylistEntry`: a video reference within a playlist.
- `CatalogRequest`: page size, continuation token and optional search query.
- `CatalogProvider`: optional capability contract for catalog operations.

## Included YouTube adapter

`YouTubeDataApiCatalogProvider` implements `CatalogProvider` using documented public YouTube Data API v3 resources:

- `search.list` with `type=channel` or `type=playlist`;
- `channels.list` for public channel metadata and available statistics;
- `playlists.list` for playlist metadata;
- `playlistItems.list` for paginated playlist entries.

The adapter maps provider responses into Lynox's provider-neutral models. Hidden or unavailable statistics remain null. It does not extract media streams or implement playback.

## Use through LynoxEngine

Create one platform-specific Ktor client and inject the catalog adapter:

```kotlin
val catalog = YouTubeDataApiCatalogProvider(
    httpClient = httpClient,
    apiKey = restrictedYouTubeApiKey
)

val engine = LynoxEngine(
    searchProvider = videoProvider,
    metadataProvider = videoProvider,
    catalogProvider = catalog
)

val channels = engine.searchChannels(CatalogRequest(query = "jazz"))
val playlists = engine.searchPlaylists(CatalogRequest(query = "jazz"))
val channel = engine.getChannel("CHANNEL_ID")
val playlist = engine.getPlaylist("PLAYLIST_ID")
val entries = engine.getPlaylistEntries("PLAYLIST_ID")
```

All operations are suspending functions and return `EngineResult`. Paginated responses expose `nextPageToken`; pass it as `CatalogRequest(pageToken = ...)` to request the next page.

The catalog provider is optional. Existing applications that only configure video search and metadata remain source-compatible because the new constructor argument is optional and appended after the existing arguments.

## Key handling and upstream policy

Do not commit API keys. A key embedded in a distributed app cannot be treated as a secret; restrict it to the required APIs and app identities where supported, monitor quota, and rotate it if exposed. Provider implementations must use authorized interfaces and comply with the upstream service terms and policies.
