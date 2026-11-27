package dev.lynox.engine.provider

import dev.lynox.engine.catalog.CatalogProvider
import dev.lynox.engine.catalog.CatalogRequest
import dev.lynox.engine.catalog.Channel
import dev.lynox.engine.catalog.Playlist
import dev.lynox.engine.catalog.PlaylistEntry
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.SearchPage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Official YouTube Data API v3 adapter for public channel and playlist catalog data.
 *
 * This is an in-process library adapter. The application supplies its own Ktor client
 * and restricted API key; Lynox Engine does not require a Lynox-hosted server.
 */
class YouTubeDataApiCatalogProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val regionCode: String? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : CatalogProvider {

    init {
        require(apiKey.isNotBlank()) { "YouTube Data API key cannot be blank" }
    }

    override suspend fun searchChannels(
        request: CatalogRequest
    ): EngineResult<SearchPage<Channel>> {
        val query = request.query?.trim().orEmpty()
        if (query.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Channel search query cannot be blank"))
        }
        return requestSafely {
            val root = when (val response = getRoot("search") {
                parameter("part", "snippet")
                parameter("type", "channel")
                parameter("q", query)
                parameter("maxResults", request.pageSize)
                request.pageToken?.let { parameter("pageToken", it) }
                regionCode?.let { parameter("regionCode", it) }
            }) {
                is EngineResult.Success -> response.value
                is EngineResult.Failure -> return@requestSafely response
            }

            EngineResult.Success(
                SearchPage(
                    items = root.array("items").mapNotNull { element ->
                        val item = element.jsonObject
                        val id = item["id"]?.jsonObject?.string("channelId") ?: return@mapNotNull null
                        val snippet = item["snippet"]?.jsonObject ?: return@mapNotNull null
                        snippet.toChannel(id)
                    },
                    nextPageToken = root.string("nextPageToken")
                )
            )
        }
    }

    override suspend fun getChannel(channelId: String): EngineResult<Channel> {
        if (channelId.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Channel ID cannot be blank"))
        }
        return requestSafely {
            val root = when (val response = getRoot("channels") {
                parameter("part", "snippet,statistics")
                parameter("id", channelId.trim())
            }) {
                is EngineResult.Success -> response.value
                is EngineResult.Failure -> return@requestSafely response
            }

            val item = root.array("items").firstOrNull()?.jsonObject
                ?: return@requestSafely EngineResult.Failure(
                    EngineError.ProviderFailure("CHANNEL_NOT_FOUND", "No public channel data was returned")
                )
            val snippet = item["snippet"]?.jsonObject
                ?: return@requestSafely EngineResult.Failure(EngineError.Unknown)
            val statistics = item["statistics"]?.jsonObject
            val base = snippet.toChannel(item.string("id") ?: channelId.trim())
            EngineResult.Success(
                base.copy(
                    subscriberCount = statistics?.long("subscriberCount"),
                    videoCount = statistics?.long("videoCount")
                )
            )
        }
    }

    override suspend fun searchPlaylists(
        request: CatalogRequest
    ): EngineResult<SearchPage<Playlist>> {
        val query = request.query?.trim().orEmpty()
        if (query.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Playlist search query cannot be blank"))
        }
        return requestSafely {
            val root = when (val response = getRoot("search") {
                parameter("part", "snippet")
                parameter("type", "playlist")
                parameter("q", query)
                parameter("maxResults", request.pageSize)
                request.pageToken?.let { parameter("pageToken", it) }
                regionCode?.let { parameter("regionCode", it) }
            }) {
                is EngineResult.Success -> response.value
                is EngineResult.Failure -> return@requestSafely response
            }

            EngineResult.Success(
                SearchPage(
                    items = root.array("items").mapNotNull { element ->
                        val item = element.jsonObject
                        val id = item["id"]?.jsonObject?.string("playlistId") ?: return@mapNotNull null
                        val snippet = item["snippet"]?.jsonObject ?: return@mapNotNull null
                        snippet.toPlaylist(id)
                    },
                    nextPageToken = root.string("nextPageToken")
                )
            )
        }
    }

    override suspend fun getPlaylist(playlistId: String): EngineResult<Playlist> {
        if (playlistId.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Playlist ID cannot be blank"))
        }
        return requestSafely {
            val root = when (val response = getRoot("playlists") {
                parameter("part", "snippet,contentDetails")
                parameter("id", playlistId.trim())
            }) {
                is EngineResult.Success -> response.value
                is EngineResult.Failure -> return@requestSafely response
            }

            val item = root.array("items").firstOrNull()?.jsonObject
                ?: return@requestSafely EngineResult.Failure(
                    EngineError.ProviderFailure("PLAYLIST_NOT_FOUND", "No public playlist data was returned")
                )
            val snippet = item["snippet"]?.jsonObject
                ?: return@requestSafely EngineResult.Failure(EngineError.Unknown)
            val base = snippet.toPlaylist(item.string("id") ?: playlistId.trim())
            val count = item["contentDetails"]?.jsonObject?.int("itemCount")
            EngineResult.Success(base.copy(itemCount = count ?: base.itemCount))
        }
    }

    override suspend fun getPlaylistEntries(
        playlistId: String,
        request: CatalogRequest
    ): EngineResult<SearchPage<PlaylistEntry>> {
        if (playlistId.isBlank()) {
            return EngineResult.Failure(EngineError.InvalidRequest("Playlist ID cannot be blank"))
        }
        return requestSafely {
            val root = when (val response = getRoot("playlistItems") {
                parameter("part", "snippet,contentDetails")
                parameter("playlistId", playlistId.trim())
                parameter("maxResults", request.pageSize)
                request.pageToken?.let { parameter("pageToken", it) }
            }) {
                is EngineResult.Success -> response.value
                is EngineResult.Failure -> return@requestSafely response
            }

            EngineResult.Success(
                SearchPage(
                    items = root.array("items").mapNotNull { element ->
                        val item = element.jsonObject
                        val snippet = item["snippet"]?.jsonObject ?: return@mapNotNull null
                        val content = item["contentDetails"]?.jsonObject
                        val mediaId = content?.string("videoId")
                            ?: snippet["resourceId"]?.jsonObject?.string("videoId")
                            ?: return@mapNotNull null
                        val title = snippet.string("title") ?: "Untitled"
                        PlaylistEntry(
                            id = item.string("id") ?: mediaId,
                            mediaId = mediaId,
                            title = title,
                            position = snippet.int("position"),
                            thumbnailUrl = snippet.thumbnailUrl(),
                            webUrl = "https://www.youtube.com/watch?v=$mediaId"
                        )
                    },
                    nextPageToken = root.string("nextPageToken")
                )
            )
        }
    }

    private suspend fun getRoot(
        resource: String,
        configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit
    ): EngineResult<JsonObject> {
        val response = httpClient.get("$BASE_URL/$resource") {
            parameter("key", apiKey)
            configure()
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            return EngineResult.Failure(mapHttpError(response.status.value, body))
        }
        return EngineResult.Success(json.parseToJsonElement(body).jsonObject)
    }

    private suspend fun <T> requestSafely(
        block: suspend () -> EngineResult<T>
    ): EngineResult<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        EngineResult.Failure(EngineError.Network("YouTube Data API request failed"))
    }

    private fun mapHttpError(status: Int, body: String): EngineError {
        val message = body.take(300)
        return when (status) {
            400 -> EngineError.InvalidRequest(message)
            401 -> EngineError.Unauthorized(message)
            403 -> if (body.contains("quota", ignoreCase = true)) {
                EngineError.QuotaExceeded()
            } else {
                EngineError.Unauthorized(message)
            }
            else -> EngineError.ProviderFailure("HTTP_$status", message)
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.content

    private fun JsonObject.long(key: String): Long? =
        this[key]?.jsonPrimitive?.content?.toLongOrNull()

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.content?.toIntOrNull()

    private fun JsonObject.array(key: String) =
        this[key]?.jsonArray.orEmpty()

    private fun JsonObject.thumbnailUrl(): String? =
        this["thumbnails"]?.jsonObject?.let { thumbnails ->
            thumbnails["high"]?.jsonObject?.string("url")
                ?: thumbnails["medium"]?.jsonObject?.string("url")
                ?: thumbnails["default"]?.jsonObject?.string("url")
        }

    private fun JsonObject.toChannel(id: String): Channel = Channel(
        id = id,
        title = string("title") ?: "Untitled",
        description = string("description"),
        thumbnailUrl = thumbnailUrl(),
        customUrl = string("customUrl"),
        webUrl = "https://www.youtube.com/channel/$id"
    )

    private fun JsonObject.toPlaylist(id: String): Playlist = Playlist(
        id = id,
        title = string("title") ?: "Untitled",
        description = string("description"),
        channelId = string("channelId"),
        channelTitle = string("channelTitle"),
        thumbnailUrl = thumbnailUrl(),
        webUrl = "https://www.youtube.com/playlist?list=$id"
    )

    private companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3"
    }
}
