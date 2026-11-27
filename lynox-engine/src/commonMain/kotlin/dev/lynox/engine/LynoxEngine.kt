package dev.lynox.engine

import dev.lynox.engine.cache.LruTtlCache
import dev.lynox.engine.catalog.CatalogProvider
import dev.lynox.engine.catalog.CatalogRequest
import dev.lynox.engine.catalog.Channel
import dev.lynox.engine.catalog.Playlist
import dev.lynox.engine.catalog.PlaylistEntry
import dev.lynox.engine.config.LynoxConfig
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.MediaItem
import dev.lynox.engine.model.SearchPage
import dev.lynox.engine.model.SearchRequest

/**
 * Main entry point for the Lynox Engine SDK.
 *
 * Provider implementations are injected by the consuming application.
 * Successful search and metadata responses are cached in memory according to [config].
 */
class LynoxEngine(
    private val searchProvider: SearchProvider? = null,
    private val metadataProvider: MetadataProvider? = null,
    config: LynoxConfig = LynoxConfig(),
    private val catalogProvider: CatalogProvider? = null
) {
    private val searchCache = LruTtlCache<String, SearchPage<MediaItem>>(
        maxEntries = config.maxCacheEntries,
        ttl = config.cacheTtl
    )
    private val metadataCache = LruTtlCache<String, MediaItem>(
        maxEntries = config.maxCacheEntries,
        ttl = config.cacheTtl
    )

    suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>> {
        if (request.query.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Search query cannot be blank"))
        }
        val provider = searchProvider
            ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)

        val cacheKey = "${request.query.trim()}|${request.pageToken.orEmpty()}|${request.pageSize}"
        searchCache.get(cacheKey)?.let { return EngineResult.Success(it) }

        return when (val result = provider.search(request)) {
            is EngineResult.Success -> {
                searchCache.put(cacheKey, result.value)
                result
            }
            is EngineResult.Failure -> result
        }
    }

    suspend fun getMediaInfo(mediaId: String): EngineResult<MediaItem> {
        if (mediaId.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Media ID cannot be blank"))
        }
        val provider = metadataProvider
            ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)

        val normalizedId = mediaId.trim()
        metadataCache.get(normalizedId)?.let { return EngineResult.Success(it) }

        return when (val result = provider.getMediaInfo(normalizedId)) {
            is EngineResult.Success -> {
                metadataCache.put(normalizedId, result.value)
                result
            }
            is EngineResult.Failure -> result
        }
    }

    /** Searches public channels using the configured catalog provider. */
    suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>> {
        val query = request.query?.trim().orEmpty()
        if (query.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Channel search query cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        return provider.searchChannels(request.copy(query = query))
    }

    suspend fun getChannel(channelId: String): EngineResult<Channel> {
        val id = channelId.trim()
        if (id.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Channel ID cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        return provider.getChannel(id)
    }

    suspend fun searchPlaylists(request: CatalogRequest): EngineResult<SearchPage<Playlist>> {
        val query = request.query?.trim().orEmpty()
        if (query.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Playlist search query cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        return provider.searchPlaylists(request.copy(query = query))
    }

    suspend fun getPlaylist(playlistId: String): EngineResult<Playlist> {
        val id = playlistId.trim()
        if (id.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Playlist ID cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        return provider.getPlaylist(id)
    }

    suspend fun getPlaylistEntries(
        playlistId: String,
        request: CatalogRequest = CatalogRequest()
    ): EngineResult<SearchPage<PlaylistEntry>> {
        val id = playlistId.trim()
        if (id.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Playlist ID cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        return provider.getPlaylistEntries(id, request.copy(query = null))
    }

    /** Removes all in-memory cached search and metadata responses. */
    suspend fun clearCache() {
        searchCache.clear()
        metadataCache.clear()
    }
}
