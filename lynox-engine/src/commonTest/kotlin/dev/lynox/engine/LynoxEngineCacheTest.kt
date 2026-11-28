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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.minutes

class LynoxEngineCacheTest {
    @Test
    fun invalidVideoSearchPageSizeIsRejectedAtRequestConstruction() {
        assertFailsWith<IllegalArgumentException> {
            SearchRequest(query = "ambient", pageSize = 0)
        }
    }

    @Test
    fun successfulSearchIsCachedUntilCacheIsCleared() = runTest {
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
    fun videoSearchNormalizesQueryBeforeProviderAndCacheLookup() = runTest {
        var calls = 0
        var receivedQuery: String? = null
        val provider = object : SearchProvider {
            override suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>> {
                calls++
                receivedQuery = request.query
                return EngineResult.Success(SearchPage(listOf(MediaItem("id-1", "Title"))))
            }
        }
        val engine = LynoxEngine(searchProvider = provider)

        assertIs<EngineResult.Success<*>>(engine.search(SearchRequest("  ambient  ")))
        assertIs<EngineResult.Success<*>>(engine.search(SearchRequest("ambient")))

        assertEquals("ambient", receivedQuery)
        assertEquals(1, calls)
    }

    @Test
    fun searchCacheDoesNotCollideWhenQueryAndPageTokenContainSeparators() = runTest {
        var calls = 0
        val provider = object : SearchProvider {
            override suspend fun search(request: SearchRequest): EngineResult<SearchPage<MediaItem>> {
                calls++
                return EngineResult.Success(SearchPage(listOf(MediaItem("result-$calls", request.query))))
            }
        }
        val engine = LynoxEngine(searchProvider = provider)

        val first = assertIs<EngineResult.Success<*>>(
            engine.search(SearchRequest(query = "a|b", pageToken = "c"))
        )
        val second = assertIs<EngineResult.Success<*>>(
            engine.search(SearchRequest(query = "a", pageToken = "b|c"))
        )

        assertEquals("result-1", (first.value as SearchPage<*>).items.single().let { (it as MediaItem).id })
        assertEquals("result-2", (second.value as SearchPage<*>).items.single().let { (it as MediaItem).id })
        assertEquals(2, calls)
    }

    @Test
    fun failedSearchIsNotCached() = runTest {
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
    fun failedMetadataLookupIsNotCached() = runTest {
        var calls = 0
        val provider = object : MetadataProvider {
            override suspend fun getMediaInfo(mediaId: String): EngineResult<MediaItem> {
                calls++
                return EngineResult.Failure(EngineError.Network("temporary failure"))
            }
        }
        val engine = LynoxEngine(metadataProvider = provider)

        assertIs<EngineResult.Failure>(engine.getMediaInfo("video-1"))
        assertIs<EngineResult.Failure>(engine.getMediaInfo("video-1"))
        assertEquals(2, calls)
    }

    @Test
    fun metadataIsCachedByNormalizedId() = runTest {
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
    fun successfulCatalogSearchIsCachedAndCleared() = runTest {
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
    fun playlistEntriesCacheIsScopedToPlaylistIdAndOpaquePageToken() = runTest {
        var calls = 0
        val catalog = object : CatalogProvider {
            override suspend fun searchChannels(request: CatalogRequest) =
                EngineResult.Success(SearchPage(listOf(Channel("channel", "Channel"))))
            override suspend fun getChannel(channelId: String) =
                EngineResult.Success(Channel(channelId, "Channel"))
            override suspend fun searchPlaylists(request: CatalogRequest) =
                EngineResult.Success(SearchPage(listOf(Playlist("playlist", "Playlist"))))
            override suspend fun getPlaylist(playlistId: String) =
                EngineResult.Success(Playlist(playlistId, "Playlist"))
            override suspend fun getPlaylistEntries(
                playlistId: String,
                request: CatalogRequest
            ): EngineResult<SearchPage<PlaylistEntry>> {
                calls++
                return EngineResult.Success(
                    SearchPage(listOf(PlaylistEntry("entry-$playlistId", "video-$calls", "Video")))
                )
            }
        }
        val engine = LynoxEngine(catalogProvider = catalog)

        val first = assertIs<EngineResult.Success<*>>(
            engine.getPlaylistEntries("playlist-A", CatalogRequest(pageToken = "opaque|token"))
        )
        val second = assertIs<EngineResult.Success<*>>(
            engine.getPlaylistEntries("playlist-B", CatalogRequest(pageToken = "opaque|token"))
        )
        val cachedFirst = assertIs<EngineResult.Success<*>>(
            engine.getPlaylistEntries("playlist-A", CatalogRequest(pageToken = "opaque|token"))
        )

        assertEquals(2, calls)
        assertEquals("video-1", ((first.value as SearchPage<*>).items.single() as PlaylistEntry).mediaId)
        assertEquals("video-2", ((second.value as SearchPage<*>).items.single() as PlaylistEntry).mediaId)
        assertEquals("video-1", ((cachedFirst.value as SearchPage<*>).items.single() as PlaylistEntry).mediaId)
    }

    @Test
    fun catalogOperationsAreExposedThroughEngine() = runTest {
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


    @Test
    fun failedCatalogSearchIsNotCached() = runTest {
        var calls = 0
        val catalog = object : CatalogProvider {
            override suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>> {
                calls++
                return EngineResult.Failure(EngineError.Network("temporary failure"))
            }
            override suspend fun getChannel(channelId: String) =
                EngineResult.Failure(EngineError.Network())
            override suspend fun searchPlaylists(request: CatalogRequest) =
                EngineResult.Failure(EngineError.Network())
            override suspend fun getPlaylist(playlistId: String) =
                EngineResult.Failure(EngineError.Network())
            override suspend fun getPlaylistEntries(playlistId: String, request: CatalogRequest) =
                EngineResult.Failure(EngineError.Network())
        }
        val engine = LynoxEngine(catalogProvider = catalog)
        val request = CatalogRequest(query = "science")

        assertIs<EngineResult.Failure>(engine.searchChannels(request))
        assertIs<EngineResult.Failure>(engine.searchChannels(request))
        assertEquals(2, calls)
    }

    @Test
    fun blankCatalogIdentifiersAreRejectedBeforeProviderCalls() = runTest {
        var calls = 0
        val catalog = object : CatalogProvider {
            override suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>> {
                calls++
                return EngineResult.Success(SearchPage(emptyList()))
            }
            override suspend fun getChannel(channelId: String): EngineResult<Channel> {
                calls++
                return EngineResult.Success(Channel(channelId, "Channel"))
            }
            override suspend fun searchPlaylists(request: CatalogRequest): EngineResult<SearchPage<Playlist>> {
                calls++
                return EngineResult.Success(SearchPage(emptyList()))
            }
            override suspend fun getPlaylist(playlistId: String): EngineResult<Playlist> {
                calls++
                return EngineResult.Success(Playlist(playlistId, "Playlist"))
            }
            override suspend fun getPlaylistEntries(
                playlistId: String,
                request: CatalogRequest
            ): EngineResult<SearchPage<PlaylistEntry>> {
                calls++
                return EngineResult.Success(SearchPage(emptyList()))
            }
        }
        val engine = LynoxEngine(catalogProvider = catalog)

        assertIs<EngineResult.Failure>(engine.getChannel("  "))
        assertIs<EngineResult.Failure>(engine.getPlaylist("  "))
        assertIs<EngineResult.Failure>(engine.getPlaylistEntries("  "))
        assertEquals(0, calls)
    }

    @Test
    fun failedCatalogOperationsAreNeverCached() = runTest {
        val calls = mutableMapOf<String, Int>()
        fun record(operation: String) {
            calls[operation] = (calls[operation] ?: 0) + 1
        }
        val catalog = object : CatalogProvider {
            override suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>> {
                record("searchChannels")
                return EngineResult.Failure(EngineError.Network("temporary"))
            }
            override suspend fun getChannel(channelId: String): EngineResult<Channel> {
                record("getChannel")
                return EngineResult.Failure(EngineError.Network("temporary"))
            }
            override suspend fun searchPlaylists(request: CatalogRequest): EngineResult<SearchPage<Playlist>> {
                record("searchPlaylists")
                return EngineResult.Failure(EngineError.Network("temporary"))
            }
            override suspend fun getPlaylist(playlistId: String): EngineResult<Playlist> {
                record("getPlaylist")
                return EngineResult.Failure(EngineError.Network("temporary"))
            }
            override suspend fun getPlaylistEntries(
                playlistId: String,
                request: CatalogRequest
            ): EngineResult<SearchPage<PlaylistEntry>> {
                record("getPlaylistEntries")
                return EngineResult.Failure(EngineError.Network("temporary"))
            }
        }
        val engine = LynoxEngine(catalogProvider = catalog)

        repeat(2) {
            assertIs<EngineResult.Failure>(engine.searchChannels(CatalogRequest(query = "science")))
            assertIs<EngineResult.Failure>(engine.getChannel("channel-1"))
            assertIs<EngineResult.Failure>(engine.searchPlaylists(CatalogRequest(query = "music")))
            assertIs<EngineResult.Failure>(engine.getPlaylist("playlist-1"))
            assertIs<EngineResult.Failure>(engine.getPlaylistEntries("playlist-1"))
        }

        assertEquals(
            mapOf(
                "searchChannels" to 2,
                "getChannel" to 2,
                "searchPlaylists" to 2,
                "getPlaylist" to 2,
                "getPlaylistEntries" to 2
            ),
            calls
        )
    }

    @Test
    fun blankCatalogSearchQueriesAreRejectedBeforeProviderCalls() = runTest {
        var calls = 0
        val catalog = object : CatalogProvider {
            override suspend fun searchChannels(request: CatalogRequest): EngineResult<SearchPage<Channel>> {
                calls++
                return EngineResult.Success(SearchPage(emptyList()))
            }
            override suspend fun getChannel(channelId: String) = EngineResult.Success(Channel(channelId, "Channel"))
            override suspend fun searchPlaylists(request: CatalogRequest): EngineResult<SearchPage<Playlist>> {
                calls++
                return EngineResult.Success(SearchPage(emptyList()))
            }
            override suspend fun getPlaylist(playlistId: String) = EngineResult.Success(Playlist(playlistId, "Playlist"))
            override suspend fun getPlaylistEntries(playlistId: String, request: CatalogRequest) =
                EngineResult.Success(SearchPage(emptyList<PlaylistEntry>()))
        }
        val engine = LynoxEngine(catalogProvider = catalog)

        assertIs<EngineResult.Failure>(engine.searchChannels(CatalogRequest(query = "  ")))
        assertIs<EngineResult.Failure>(engine.searchPlaylists(CatalogRequest(query = "\t")))
        assertEquals(0, calls)
    }

}
