package dev.lynox.engine

import dev.lynox.engine.cache.LruTtlCache
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
    config: LynoxConfig = LynoxConfig()
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

        val cacheKey = "${request.query.trim()}| ${request.pageToken.orEmpty()}|${request.pageSize}"
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

    /** Removes all in-memory cached search and metadata responses. */
    fun clearCache() {
        searchCache.clear()
        metadataCache.clear()
    }
}
