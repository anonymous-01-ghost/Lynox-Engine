package dev.lynox.engine

import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.MediaItem
import dev.lynox.engine.model.SearchPage
import dev.lynox.engine.model.SearchRequest

/** Implementations must use provider interfaces and access methods permitted for the application. */
interface SearchProvider {
    suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>>
}

interface MetadataProvider {
    suspend fun getMediaInfo(mediaId: String): EngineResult<MediaItem>
}
