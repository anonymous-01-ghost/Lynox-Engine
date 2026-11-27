package dev.lynox.engine.provider

import dev.lynox.engine.catalog.CatalogRequest
import dev.lynox.engine.model.EngineResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

class YouTubeDataApiCatalogProviderTest {

    @Test
    fun channelSearchMapsChannelAndContinuationToken() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """
                    {
                      "nextPageToken": "channel-next",
                      "items": [{
                        "id": {"kind": "youtube#channel", "channelId": "channel-1"},
                        "snippet": {
                          "title": "Example channel",
                          "description": "Public channel",
                          "customUrl": "@example",
                          "thumbnails": {
                            "high": {"url": "https://img.example/channel.jpg"}
                          }
                        }
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiCatalogProvider(client, "test-key")

        val result = provider.searchChannels(CatalogRequest(query = "example"))

        val success = assertIs<EngineResult.Success<*>>(result)
        val page = success.value as dev.lynox.engine.model.SearchPage<*>
        assertEquals("channel-next", page.nextPageToken)
        val channel = page.items.single() as dev.lynox.engine.catalog.Channel
        assertEquals("channel-1", channel.id)
        assertEquals("Example channel", channel.title)
        assertEquals("@example", channel.customUrl)
        client.close()
    }

    @Test
    fun playlistEntriesMapVideoAndPosition() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """
                    {
                      "nextPageToken": "items-next",
                      "items": [{
                        "id": "playlist-item-1",
                        "snippet": {
                          "title": "Example video",
                          "position": 3,
                          "resourceId": {"kind": "youtube#video", "videoId": "video-1"},
                          "thumbnails": {
                            "medium": {"url": "https://img.example/video.jpg"}
                          }
                        },
                        "contentDetails": {"videoId": "video-1"}
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiCatalogProvider(client, "test-key")

        val result = provider.getPlaylistEntries("playlist-1")

        val success = assertIs<EngineResult.Success<*>>(result)
        val page = success.value as dev.lynox.engine.model.SearchPage<*>
        assertEquals("items-next", page.nextPageToken)
        val entry = page.items.single() as dev.lynox.engine.catalog.PlaylistEntry
        assertEquals("video-1", entry.mediaId)
        assertEquals(3, entry.position)
        assertEquals("playlist-item-1", entry.id)
        client.close()
    }


    @Test
    fun apiNotEnabledIsMappedConsistentlyForCatalogRequests() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"error":{"errors":[{"reason":"accessNotConfigured"}],"message":"quota is mentioned in unrelated text"}}""",
                status = HttpStatusCode.Forbidden,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiCatalogProvider(client, "test-key")

        val result = provider.searchChannels(CatalogRequest(query = "example"))

        val failure = assertIs<EngineResult.Failure>(result)
        val error = assertIs<dev.lynox.engine.model.EngineError.ProviderFailure>(failure.error)
        assertEquals("API_NOT_ENABLED", error.code)
        client.close()
    }

    @Test
    fun blankChannelQueryDoesNotCallNetwork() = runBlocking {
        val client = HttpClient(MockEngine {
            error("Blank channel query must not trigger a network request")
        })
        val provider = YouTubeDataApiCatalogProvider(client, "test-key")

        val result = provider.searchChannels(CatalogRequest(query = "  "))

        val failure = assertIs<EngineResult.Failure>(result)
        assertIs<dev.lynox.engine.model.EngineError.InvalidRequest>(failure.error)
        client.close()
    }

    @Test
    fun malformedJsonIsMappedToInvalidProviderResponse() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """{ malformed json""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiCatalogProvider(client, "test-key")

        val result = provider.searchChannels(CatalogRequest(query = "example"))

        val failure = assertIs<EngineResult.Failure>(result)
        val error = assertIs<dev.lynox.engine.model.EngineError.ProviderFailure>(failure.error)
        assertEquals("INVALID_RESPONSE", error.code)
        client.close()
    }

    @Test
    fun httpErrorDoesNotExposeUpstreamResponseBody() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"error":{"message":"private-sensitive-detail"}}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiCatalogProvider(client, "test-key")

        val result = provider.searchChannels(CatalogRequest(query = "example"))

        val failure = assertIs<EngineResult.Failure>(result)
        assertFalse(failure.error.toString().contains("private-sensitive-detail"))
        client.close()
    }


    @Test
    fun channelSearchPropagatesCoroutineCancellation() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            throw CancellationException("cancel channel search")
        })
        val provider = YouTubeDataApiCatalogProvider(client, "test-key")

        try {
            assertFailsWith<CancellationException> {
                provider.searchChannels(CatalogRequest(query = "example"))
            }
            assertEquals(1, calls)
        } finally {
            client.close()
        }
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, "application/json")
}
