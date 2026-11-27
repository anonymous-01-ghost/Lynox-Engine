package dev.lynox.engine

import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.MediaItem
import dev.lynox.engine.model.SearchPage
import dev.lynox.engine.model.SearchRequest

/**
 * Main entry point for the Lynox Engine SDK.
 *
 * Provider implementations are injected by the consuming application.
 * This keeps the shared engine independent from undocumented provider endpoints.
 */
class LynoxEngine(
    private val searchProvider: SearchProvider? = null,
    private val metadataProvider: MetadataProvider? = null
) {
    suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>> {
        val provider = searchProvider
            ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        if (request.query.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Search query cannot be blank"))
        }
        return provider.search(request)
    }

    suspend fun getMediaInfo(mediaId: String): EngineResult<MediaItem> {
        val provider = metadataProvider
            ?: return EngineResult.Failure(EngineError.ProviderNotConfigured)
        if (mediaId.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Media ID cannot be blank"))
        }
        return provider.getMediaInfo(mediaId)
    }
}
