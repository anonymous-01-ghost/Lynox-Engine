package dev.lynox.engine.provider

import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.SearchRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
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

class YouTubeDataApiProviderTest {

    @Test
    fun searchMapsVideoMetadataAndContinuationToken() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """
                    {
                      "nextPageToken": "next-2",
                      "items": [{
                        "id": {"videoId": "video-123"},
                        "snippet": {
                          "title": "Example video",
                          "description": "Description",
                          "channelId": "channel-1",
                          "channelTitle": "Example channel",
                          "thumbnails": {
                            "high": {"url": "https://img.example/high.jpg"}
                          }
                        }
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.search(SearchRequest("example"))

        val success = assertIs<EngineResult.Success<*>>(result)
        val page = success.value as dev.lynox.engine.model.SearchPage<*>
        assertEquals("next-2", page.nextPageToken)
        val item = page.items.single() as dev.lynox.engine.model.MediaItem
        assertEquals("video-123", item.id)
        assertEquals("Example video", item.title)
        assertEquals("https://img.example/high.jpg", item.thumbnailUrl)
        client.close()
    }

    @Test
    fun directProviderSearchTrimsQueryBeforeSendingRequest() = runBlocking {
        var sentQuery: String? = null
        val client = HttpClient(MockEngine { request ->
            sentQuery = request.url.parameters["q"]
            respond(
                content = """{"items": []}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.search(SearchRequest("  ambient  "))

        assertIs<EngineResult.Success<*>>(result)
        assertEquals("ambient", sentQuery)
        client.close()
    }

    @Test
    fun metadataMapsIsoDurationToSecondsAndTrimsId() = runBlocking {
        var requestedId: String? = null
        val client = HttpClient(MockEngine { request ->
            requestedId = request.url.parameters["id"]
            respond(
                content = """
                    {
                      "items": [{
                        "id": "video-123",
                        "snippet": {
                          "title": "Example video",
                          "channelId": "channel-1",
                          "channelTitle": "Example channel"
                        },
                        "contentDetails": {"duration": "PT1H2M3S"}
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.getMediaInfo(" video-123 ")

        val success = assertIs<EngineResult.Success<*>>(result)
        val item = success.value as dev.lynox.engine.model.MediaItem
        assertEquals(3723L, item.durationSeconds)
        assertEquals("video-123", requestedId)
        assertEquals("video-123", item.id)
        client.close()
    }


    @Test
    fun quotaErrorsAreMappedToTypedFailure() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"error":{"errors":[{"reason":"quotaExceeded"}],"message":"Daily quota exhausted"}}""",
                status = HttpStatusCode.Forbidden,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.search(SearchRequest("example"))

        val failure = assertIs<EngineResult.Failure>(result)
        assertIs<dev.lynox.engine.model.EngineError.QuotaExceeded>(failure.error)
        client.close()
    }


    @Test
    fun apiNotEnabledIsNotMisclassifiedAsQuota() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"error":{"errors":[{"reason":"accessNotConfigured"}],"message":"quota is mentioned in unrelated text"}}""",
                status = HttpStatusCode.Forbidden,
                headers = jsonHeaders()
            )
        })
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.search(SearchRequest("example"))

        val failure = assertIs<EngineResult.Failure>(result)
        val error = assertIs<dev.lynox.engine.model.EngineError.ProviderFailure>(failure.error)
        assertEquals("API_NOT_ENABLED", error.code)
        client.close()
    }

    @Test
    fun blankSearchQueryReturnsInvalidRequestWithoutNetworkCall() = runBlocking {
        val client = HttpClient(MockEngine {
            error("A blank query must not trigger a network request")
        })
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.search(SearchRequest("   "))

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
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.search(SearchRequest("example"))

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
        val provider = YouTubeDataApiProvider(client, "test-key")

        val result = provider.search(SearchRequest("example"))

        val failure = assertIs<EngineResult.Failure>(result)
        assertFalse(failure.error.toString().contains("private-sensitive-detail"))
        client.close()
    }


    @Test
    fun searchPropagatesCoroutineCancellation() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            throw CancellationException("cancel search")
        })
        val provider = YouTubeDataApiProvider(client, "test-key")

        try {
            assertFailsWith<CancellationException> {
                provider.search(SearchRequest("example"))
            }
            assertEquals(1, calls)
        } finally {
            client.close()
        }
    }

    private fun jsonHeaders(): Headers =
        headersOf(HttpHeaders.ContentType, "application/json")
}
