package dev.lynox.engine.provider

import dev.lynox.engine.MetadataProvider
import dev.lynox.engine.SearchProvider
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.MediaItem
import dev.lynox.engine.model.SearchPage
import dev.lynox.engine.model.SearchRequest
import dev.lynox.engine.network.executeWithRetry
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.CancellationException

/**
 * Official YouTube Data API v3 adapter for search and public video metadata.
 *
 * The caller supplies an HttpClient configured with a platform engine and an API key.
 * This adapter does not extract media streams or implement playback.
 */
class YouTubeDataApiProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val regionCode: String? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SearchProvider, MetadataProvider {

    init {
        require(apiKey.isNotBlank()) { "YouTube Data API key cannot be blank" }
    }

    override suspend fun search(
        request: SearchRequest
    ): EngineResult<SearchPage<MediaItem>> {
        if (request.pageSize !in 1..50) {
            return EngineResult.Failure(EngineError.InvalidRequest("pageSize must be between 1 and 50"))
        }
        if (request.query.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Search query cannot be blank"))
        }

        return requestSafely {
            val response = executeWithRetry {
                httpClient.get("$BASE_URL/search") {
                    parameter("key", apiKey)
                    parameter("part", "snippet")
                    parameter("type", "video")
                    parameter("q", request.query)
                    parameter("maxResults", request.pageSize)
                    request.pageToken?.let { parameter("pageToken", it) }
                    regionCode?.let { parameter("regionCode", it) }
                }
            }

            val responseBody = response.bodyAsText()
            val root = parseResponse(response.status.value, responseBody)
                ?: return@requestSafely EngineResult.Failure(
                    mapHttpError(response.status.value, responseBody)
                )

            val items = root["items"]?.jsonArray.orEmpty().mapNotNull { element ->
                val item = element.jsonObject
                val videoId = item["id"]?.jsonObject?.string("videoId") ?: return@mapNotNull null
                val snippet = item["snippet"]?.jsonObject ?: return@mapNotNull null
                MediaItem(
                    id = videoId,
                    title = snippet.string("title") ?: "Untitled",
                    description = snippet.string("description"),
                    channelId = snippet.string("channelId"),
                    channelTitle = snippet.string("channelTitle"),
                    thumbnailUrl = snippet["thumbnails"]?.jsonObject
                        ?.get("high")?.jsonObject?.string("url")
                        ?: snippet["thumbnails"]?.jsonObject
                            ?.get("default")?.jsonObject?.string("url"),
                    webUrl = "https://www.youtube.com/watch?v=$videoId"
                )
            }

            EngineResult.Success(
                SearchPage(
                    items = items,
                    nextPageToken = root.string("nextPageToken")
                )
            )
        }
    }

    override suspend fun getMediaInfo(mediaId: String): EngineResult<MediaItem> {
        val normalizedId = mediaId.trim()
        if (normalizedId.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Media ID cannot be blank"))
        }

        return requestSafely {
            val response = executeWithRetry {
                httpClient.get("$BASE_URL/videos") {
                    parameter("key", apiKey)
                    parameter("part", "snippet,contentDetails")
                    parameter("id", normalizedId)
                }
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                return@requestSafely EngineResult.Failure(
                    mapHttpError(response.status.value, body)
                )
            }

            val root = json.parseToJsonElement(body).jsonObject
            val item = root["items"]?.jsonArray?.firstOrNull()?.jsonObject
                ?: return@requestSafely EngineResult.Failure(
                    EngineError.ProviderFailure("VIDEO_NOT_FOUND", "No public video metadata was returned")
                )
            val snippet = item["snippet"]?.jsonObject
                ?: return@requestSafely EngineResult.Failure(EngineError.Unknown)
            val contentDetails = item["contentDetails"]?.jsonObject
            val thumbnails = snippet["thumbnails"]?.jsonObject

            EngineResult.Success(
                MediaItem(
                    id = normalizedId,
                    title = snippet.string("title") ?: "Untitled",
                    description = snippet.string("description"),
                    channelId = snippet.string("channelId"),
                    channelTitle = snippet.string("channelTitle"),
                    durationSeconds = contentDetails?.string("duration")?.toDurationSeconds(),
                    thumbnailUrl = thumbnails?.get("high")?.jsonObject?.string("url")
                        ?: thumbnails?.get("default")?.jsonObject?.string("url"),
                    webUrl = "https://www.youtube.com/watch?v=$normalizedId"
                )
            )
        }
    }

    private suspend fun <T> requestSafely(
        block: suspend () -> EngineResult<T>
    ): EngineResult<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SerializationException) {
        EngineResult.Failure(
            EngineError.ProviderFailure("INVALID_RESPONSE", "YouTube returned malformed JSON")
        )
    } catch (_: IllegalStateException) {
        EngineResult.Failure(
            EngineError.ProviderFailure("INVALID_RESPONSE", "YouTube returned an unexpected JSON structure")
        )
    } catch (error: Exception) {
        EngineResult.Failure(
            // Avoid exposing request URLs, which can contain the API key, in error messages.
            EngineError.Network("YouTube Data API request failed")
        )
    }

    private fun parseResponse(status: Int, body: String): JsonObject? {
        if (status !in 200..299) return null
        return json.parseToJsonElement(body).jsonObject
    }

    private fun mapHttpError(status: Int, body: String?): EngineError {
        val isQuotaError = body?.contains("quota", ignoreCase = true) == true
        return when (status) {
            400 -> EngineError.InvalidRequest("YouTube rejected the request (HTTP 400)")
            401 -> EngineError.Unauthorized("YouTube API authentication failed")
            403 -> if (isQuotaError) {
                EngineError.QuotaExceeded()
            } else {
                EngineError.Unauthorized("YouTube API access was denied")
            }
            else -> EngineError.ProviderFailure("HTTP_$status", "YouTube request failed (HTTP $status)")
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.content

    private fun String.toDurationSeconds(): Long? {
        val match = DURATION_PATTERN.matchEntire(this) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: 0L
        val minutes = match.groupValues[2].toLongOrNull() ?: 0L
        val seconds = match.groupValues[3].toLongOrNull() ?: 0L
        return hours * 3600L + minutes * 60L + seconds
    }

    private companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3"
        val DURATION_PATTERN = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
    }
}
