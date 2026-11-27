# Provider-neutral catalog contracts

Lynox Engine is a Kotlin Multiplatform **library**, not a hosted API or a service that requires a Lynox server.

The optional `dev.lynox.engine.catalog` contracts define normalized models for:

- channels (`Channel`);
- playlists (`Playlist`);
- playlist entries (`PlaylistEntry`);
- paginated catalog requests (`CatalogRequest`).

A provider implements `CatalogProvider` when it supports these capabilities. Applications inject that implementation directly into their Lynox Engine integration. Video search and metadata remain separate capabilities, so a provider is not forced to implement every operation.

## Example provider contract

```kotlin
class MyCatalogProvider : CatalogProvider {
    override suspend fun searchChannels(
        request: CatalogRequest
    ): EngineResult<SearchPage<Channel>> = TODO()

    override suspend fun getChannel(
        channelId: String
    ): EngineResult<Channel> = TODO()

    override suspend fun searchPlaylists(
        request: CatalogRequest
    ): EngineResult<SearchPage<Playlist>> = TODO()

    override suspend fun getPlaylist(
        playlistId: String
    ): EngineResult<Playlist> = TODO()

    override suspend fun getPlaylistEntries(
        playlistId: String,
        request: CatalogRequest
    ): EngineResult<SearchPage<PlaylistEntry>> = TODO()
}
```

This commit establishes the provider-neutral public contract only. A concrete provider adapter, caching in the engine facade, and platform integration tests are subsequent implementation tasks. No server-side Lynox component is introduced.

Provider implementations must use documented, authorized interfaces and respect the terms and policies of the upstream service. These catalog contracts do not provide media stream extraction or playback.
