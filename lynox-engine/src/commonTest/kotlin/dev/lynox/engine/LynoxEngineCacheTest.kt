package dev.lynox.engine

import dev.lynox.engine.config.LynoxConfig
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.MediaItem
import dev.lynox.engine.model.SearchPage
import dev.lynox.engine.model.SearchRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes

class LynoxEngineCacheTest {
    @Test
    fun successfulSearchIsCachedUntilCacheIsCleared() = runBlocking {
        var calls = 0
        val provider = object : SearchProvider {
            override suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>> {
                calls++
                return EngineResult.Success(SearchPage(listOf(MediaItem("id-1", "Title"))))
            }
        }
        val engine = LynoxEngine(
            searchProvider = provider,
            config = LynoxConfig(cacheTtl = 5.minutes, maxCacheEntries = 10)
        )
        val request = SearchRequest("  ambient  ")

        assertIs<EngineResult.Success<*>>(engine.search(request))
        assertIs<EngineResult.Success<*>>(engine.search(request))
        assertEquals(1, calls)

        engine.clearCache()
        assertIs<EngineResult.Success<*>>(engine.search(request))
        assertEquals(2, calls)
    }

    @Test
    fun failedSearchIsNotCached() = runBlocking {
        var calls = 0
        val provider = object : SearchProvider {
            override suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>> {
                calls++
                return EngineResult.Failure(EngineError.Network())
            }
        }
        val engine = LynoxEngine(searchProvider = provider)
        val request = SearchRequest("ambient")

        assertIs<EngineResult.Failure>(engine.search(request))
        assertIs<EngineResult.Failure>(engine.search(request))
        assertEquals(2, calls)
    }

    @Test
    fun metadataIsCachedByNormalizedId() = runBlocking {
        var calls = 0
        val provider = object : MetadataProvider {
            override suspend fun getMediaInfo(mediaId: String): EngineResult<MediaItem> {
                calls++
                return EngineResult.Success(MediaItem(mediaId, "Title"))
            }
        }
        val engine = LynoxEngine(metadataProvider = provider)

        assertIs<EngineResult.Success<*>>(engine.getMediaInfo(" video-1 "))
        assertIs<EngineResult.Success<*>>(engine.getMediaInfo("video-1"))
        assertEquals(1, calls)
    }
}
