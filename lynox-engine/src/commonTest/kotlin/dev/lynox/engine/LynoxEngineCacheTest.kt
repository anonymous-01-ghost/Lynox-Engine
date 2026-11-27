package dev.lynox.engine

import dev.lynox.engine.config.LynoxConfig
import dev.lynox.engine.catalog.CatalogProvider
import dev.lynox.engine.catalog.CatalogRequest
import dev.lynox.engine.catalog.Channel
import dev.lynox.engine.catalog.Playlist
import dev.lynox.engine.catalog.PlaylistEntry
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
    @Test
    fun successfulCatalogSearchIsCachedAndCleared() = runBlocking {
        var calls = 0
        val catalog = object : CatalogProvider {
            override suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>> {
                calls++
                return EngineResult.Success(SearchPage(listOf(Channel("channel-1", "Channel"))))
            }
            override suspend fun getChannel(channelId: String) = EngineResult.Success(Channel(channelId, "Channel"))
            override suspend fun searchPlaylists(request: CatalogRequest) = EngineResult.Success(SearchPage(listOf(Playlist("playlist-1", "Playlist"))))
            override suspend fun getPlaylist(playlistId: String) = EngineResult.Success(Playlist(playlistId, "Playlist"))
            override suspend fun getPlaylistEntries(playlistId: String, request: CatalogRequest) =
                EngineResult.Success(SearchPage(listOf(PlaylistEntry("entry-1", "video-1", "Video"))))
        }
        val engine = LynoxEngine(catalogProvider = catalog)
        val request = CatalogRequest(query = "science")

        assertIs<EngineResult.Success<*>>(engine.searchChannels(request))
        assertIs<EngineResult.Success<*>>(engine.searchChannels(request))
        assertEquals(1, calls)

        engine.clearCache()
        assertIs<EngineResult.Success<*>>(engine.searchChannels(request))
        assertEquals(2, calls)
    }

    @Test
    fun catalogOperationsAreExposedThroughEngine() = runBlocking {
        var receivedQuery: String? = null
        val catalog = object : CatalogProvider {
            override suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>> {
                receivedQuery = request.query
                return EngineResult.Success(SearchPage(listOf(Channel("channel-1", "Channel"))))
            }

            override suspend fun getChannel(channelId: String): EngineResult<Channel> =
                EngineResult.Success(Channel(channelId, "Channel"))

            override suspend fun searchPlaylists(request: CatalogRequest): EngineResult<SearchPage<Playlist>> =
                EngineResult.Success(SearchPage(listOf(Playlist("playlist-1", "Playlist"))))

            override suspend fun getPlaylist(playlistId: String): EngineResult<Playlist> =
                EngineResult.Success(Playlist(playlistId, "Playlist"))

            override suspend fun getPlaylistEntries(
                playlistId: String,
                request: CatalogRequest
            ): EngineResult<SearchPage<PlaylistEntry>> =
                EngineResult.Success(SearchPage(listOf(PlaylistEntry("entry-1", "video-1", "Video"))))
        }
        val engine = LynoxEngine(catalogProvider = catalog)

        val channels = assertIs<EngineResult.Success<*>>(
            engine.searchChannels(CatalogRequest(query = "  science  "))
        )
        assertEquals("science", receivedQuery)
        assertEquals("channel-1", (channels.value as SearchPage<*>).items.single().let { (it as Channel).id })

        val playlist = assertIs<EngineResult.Success<*>>(engine.getPlaylist(" playlist-1 "))
        assertEquals("playlist-1", (playlist.value as Playlist).id)

        val entries = assertIs<EngineResult.Success<*>>(engine.getPlaylistEntries(" playlist-1 "))
        assertEquals("video-1", ((entries.value as SearchPage<*>).items.single() as PlaylistEntry).mediaId)
    }

}
