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
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun metadataMapsIsoDurationToSeconds() = runBlocking {
        val client = HttpClient(MockEngine {
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

        val result = provider.getMediaInfo("video-123")

        val success = assertIs<EngineResult.Success<*>>(result)
        val item = success.value as dev.lynox.engine.model.MediaItem
        assertEquals(3723L, item.durationSeconds)
        client.close()
    }

    private fun jsonHeaders(): Headers =
        headersOf(HttpHeaders.ContentType, "application/json")
}
