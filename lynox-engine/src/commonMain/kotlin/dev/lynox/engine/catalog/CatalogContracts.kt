package dev.lynox.engine.catalog

import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.SearchPage

/**
 * Provider-neutral channel metadata.
 *
 * Fields unavailable from a provider remain null. This contract contains no
 * provider-specific HTTP or authentication details.
 */
data class Channel(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val customUrl: String? = null,
    val subscriberCount: Long? = null,
    val videoCount: Long? = null,
    val webUrl: String? = null
)

/** Provider-neutral playlist metadata. */
data class Playlist(
    val id: String,
    val title: String,
    val description: String? = null,
    val channelId: String? = null,
    val channelTitle: String? = null,
    val itemCount: Int? = null,
    val thumbnailUrl: String? = null,
    val webUrl: String? = null
)

/** A normalized reference to an item contained in a playlist. */
data class PlaylistEntry(
    val id: String,
    val mediaId: String,
    val title: String,
    val position: Int? = null,
    val thumbnailUrl: String? = null,
    val webUrl: String? = null
)

/**
 * Common paging input for catalog searches and playlist contents.
 *
 * [pageToken] is opaque: providers must not assume a token is numeric or
 * modify it before forwarding it to their source.
 */
data class CatalogRequest(
    val query: String? = null,
    val pageToken: String? = null,
    val pageSize: Int = 20
) {
    init {
        require(pageSize in 1..50) { "pageSize must be between 1 and 50" }
    }
}

/**
 * Optional provider capability for channel and playlist catalog operations.
 *
 * Implementations should return [EngineResult.Failure] for expected upstream
 * failures. Coroutine cancellation must propagate and must not be cached as a
 * provider failure. The engine trims identifiers and search queries, validates
 * page sizes, and caches successful results only. Page tokens remain opaque.
 *
 * A provider can implement this interface independently of video search and
 * metadata. Lynox Engine itself remains a library; applications inject the
 * provider implementation they choose. Implementations must use access methods
 * permitted for the application.
 */
interface CatalogProvider {
    suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>>
    suspend fun getChannel(channelId: String): EngineResult<Channel>
    suspend fun searchPlaylists(request: CatalogRequest): EngineResult<SearchPage<Playlist>>
    suspend fun getPlaylist(playlistId: String): EngineResult<Playlist>
    suspend fun getPlaylistEntries(
        playlistId: String,
        request: CatalogRequest = CatalogRequest()
    ): EngineResult<SearchPage<PlaylistEntry>>
}
