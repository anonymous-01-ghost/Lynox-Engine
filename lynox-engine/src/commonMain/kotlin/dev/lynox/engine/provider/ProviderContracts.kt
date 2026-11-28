package dev.lynox.engine

import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.MediaItem
import dev.lynox.engine.model.SearchPage
import dev.lynox.engine.model.SearchRequest

/**
 * Supplies provider-neutral search results to [LynoxEngine].
 *
 * Implementations should return [EngineResult.Failure] for expected provider
 * failures (for example network, quota, or authorization errors). They must
 * not convert coroutine cancellation into a regular failure: cancellation
 * exceptions must propagate to the caller.
 *
 * The engine trims the query before invoking this contract. Page tokens are
 * opaque and must be passed to the upstream provider without reinterpretation.
 * Implementations must use access methods permitted for the application.
 */
interface SearchProvider {
    suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>>
}

/**
 * Supplies normalized media metadata to [LynoxEngine].
 *
 * Expected provider failures should be represented by [EngineResult.Failure].
 * Coroutine cancellation must propagate unchanged. The engine trims the
 * identifier before invoking this contract; identifiers are otherwise opaque.
 */
interface MetadataProvider {
    suspend fun getMediaInfo(mediaId: String): EngineResult<MediaItem>
}
