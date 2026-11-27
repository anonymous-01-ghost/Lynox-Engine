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
 * Successful search, metadata, and catalog responses are cached in memory according to [config].
 */
class LynoxEngine(
    private val searchProvider: SearchProvider? = null,
    private val metadataProvider: MetadataProvider? = null,
    config: LynoxConfig = LynoxConfig(),
    private val catalogProvider: CatalogProvider? = null
) {
    private val searchCache = LruTtlCache<PageCacheKey, SearchPage<MediaItem>>(
        maxEntries = config.maxCacheEntries,
        ttl = config.cacheTtl
    )
    private val metadataCache = LruTtlCache<String, MediaItem>(
        maxEntries = config.maxCacheEntries,
        ttl = config.cacheTtl
    )
    private val channelSearchCache = LruTtlCache<PageCacheKey, SearchPage<Channel>>(config.maxCacheEntries, config.cacheTtl)
    private val channelCache = LruTtlCache<String, Channel>(config.maxCacheEntries, config.cacheTtl)
    private val playlistSearchCache = LruTtlCache<PageCacheKey, SearchPage<Playlist>>(config.maxCacheEntries, config.cacheTtl)
    private val playlistCache = LruTtlCache<String, Playlist>(config.maxCacheEntries, config.cacheTtl)
    private val playlistEntriesCache = LruTtlCache<PageCacheKey, SearchPage<PlaylistEntry>>(config.maxCacheEntries, config.cacheTtl)

    suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>> {
        if (request.pageSize !in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            return EngineResult.Failure(EngineError.InvalidRequest("pageSize must be between 1 and 50"))
        }
        if (request.query.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Search query cannot be blank"))
        }
        val provider = searchProvider
            ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)

        val normalized = request.copy(query = request.query.trim())
        val cacheKey = PageCacheKey(normalized.query, normalized.pageToken, normalized.pageSize)
        searchCache.get(cacheKey)?.let { return EngineResult.Success(it) }

        return when (val result = provider.search(normalized)) {
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
        if (request.pageSize !in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            return EngineResult.Failure(EngineError.InvalidRequest("pageSize must be between 1 and 50"))
        }
        val query = request.query?.trim().orEmpty()
        if (query.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Channel search query cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        val normalized = request.copy(query = query)
        val cacheKey = PageCacheKey(query, normalized.pageToken, normalized.pageSize)
        channelSearchCache.get(cacheKey)?.let { return EngineResult.Success(it) }
        return when (val result = provider.searchChannels(normalized)) {
            is EngineResult.Success -> {
                channelSearchCache.put(cacheKey, result.value)
                result
            }
            is EngineResult.Failure -> result
        }
    }

    suspend fun getChannel(channelId: String): EngineResult<Channel> {
        val id = channelId.trim()
        if (id.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Channel ID cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        channelCache.get(id)?.let { return EngineResult.Success(it) }
        return when (val result = provider.getChannel(id)) {
            is EngineResult.Success -> {
                channelCache.put(id, result.value)
                result
            }
            is EngineResult.Failure -> result
        }
    }

    suspend fun searchPlaylists(request: CatalogRequest): EngineResult<SearchPage<Playlist>> {
        if (request.pageSize !in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            return EngineResult.Failure(EngineError.InvalidRequest("pageSize must be between 1 and 50"))
        }
        val query = request.query?.trim().orEmpty()
        if (query.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Playlist search query cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        val normalized = request.copy(query = query)
        val cacheKey = PageCacheKey(query, normalized.pageToken, normalized.pageSize)
        playlistSearchCache.get(cacheKey)?.let { return EngineResult.Success(it) }
        return when (val result = provider.searchPlaylists(normalized)) {
            is EngineResult.Success -> {
                playlistSearchCache.put(cacheKey, result.value)
                result
            }
            is EngineResult.Failure -> result
        }
    }

    suspend fun getPlaylist(playlistId: String): EngineResult<Playlist> {
        val id = playlistId.trim()
        if (id.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Playlist ID cannot be blank"))
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        playlistCache.get(id)?.let { return EngineResult.Success(it) }
        return when (val result = provider.getPlaylist(id)) {
            is EngineResult.Success -> {
                playlistCache.put(id, result.value)
                result
            }
            is EngineResult.Failure -> result
        }
    }

    suspend fun getPlaylistEntries(
        playlistId: String,
        request: CatalogRequest = CatalogRequest()
    ): EngineResult<SearchPage<PlaylistEntry>> {
        val id = playlistId.trim()
        if (id.isBlank()) return EngineResult.Failure(EngineError.InvalidRequest("Playlist ID cannot be blank"))
        if (request.pageSize !in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            return EngineResult.Failure(EngineError.InvalidRequest("pageSize must be between 1 and 50"))
        }
        val provider = catalogProvider ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        val normalized = request.copy(query = null)
        val cacheKey = PageCacheKey(id, normalized.pageToken, normalized.pageSize)
        playlistEntriesCache.get(cacheKey)?.let { return EngineResult.Success(it) }
        return when (val result = provider.getPlaylistEntries(id, normalized)) {
            is EngineResult.Success -> {
                playlistEntriesCache.put(cacheKey, result.value)
                result
            }
            is EngineResult.Failure -> result
        }
    }

    private data class PageCacheKey(
        val resourceId: String,
        val pageToken: String?,
        val pageSize: Int
    )

    private companion object {
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 50
    }

    /** Removes all in-memory search, metadata, and catalog responses. */
    suspend fun clearCache() {
        searchCache.clear()
        metadataCache.clear()
        channelSearchCache.clear()
        channelCache.clear()
        playlistSearchCache.clear()
        playlistCache.clear()
        playlistEntriesCache.clear()
    }
}
