package dev.lynox.engine.extractor

import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult

/**
 * Minimal transport contract for retrieving text manifests.
 *
 * Platform applications provide the actual HTTP implementation (for example,
 * URLSession, HttpURLConnection, or an application-owned client). The engine
 * does not create a server or require a particular networking dependency.
 */
interface ManifestHttpClient {
    suspend fun get(request: ManifestHttpRequest): ManifestHttpResponse
}

data class ManifestHttpRequest(
    val url: String,
    val maxResponseChars: Int,
    val timeoutMillis: Long
)

data class ManifestHttpResponse(
    val statusCode: Int,
    val body: String,
    val contentType: String? = null,
    /** Final URL after redirects; defaults to the requested URL. */
    val finalUrl: String? = null
)

/**
 * Loads an HLS master manifest through an injected transport and parses it.
 *
 * The transport must enforce the response-size limit while reading the body,
 * not only after buffering it. Coroutine cancellation is intentionally allowed
 * to propagate to the caller.
 */
class HlsManifestResolver(
    private val httpClient: ManifestHttpClient,
    private val parser: HlsMasterPlaylistParser = HlsMasterPlaylistParser(),
    private val maxResponseChars: Int = DEFAULT_MAX_RESPONSE_CHARS,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) : MediaResolver {

    init {
        require(maxResponseChars in 1..MAX_ALLOWED_RESPONSE_CHARS) {
            "maxResponseChars must be between 1 and $MAX_ALLOWED_RESPONSE_CHARS"
        }
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
    }

    override suspend fun resolve(sourceUrl: String): EngineResult<ResolvedMedia> {
        val url = sourceUrl.trim()
        if (!isHttpUrl(url)) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("sourceUrl must be an absolute HTTP or HTTPS URL")
            )
        }

        val response = try {
            httpClient.get(
                ManifestHttpRequest(
                    url = url,
                    maxResponseChars = maxResponseChars,
                    timeoutMillis = timeoutMillis
                )
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            return EngineResult.Failure(EngineError.Network(failure.message))
        }

        if (response.body.length > maxResponseChars) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("HLS manifest exceeds the configured response limit")
            )
        }

        when (response.statusCode) {
            in 200..299 -> Unit
            401, 403 -> return EngineResult.Failure(
                EngineError.Unauthorized("Manifest request was not authorized")
            )
            in 500..599 -> return EngineResult.Failure(
                EngineError.Network("Manifest server returned HTTP ${response.statusCode}")
            )
            else -> return EngineResult.Failure(
                EngineError.ProviderFailure(
                    code = "HTTP_${response.statusCode}",
                    message = "Manifest request failed"
                )
            )
        }

        val contentType = response.contentType?.substringBefore(';')?.trim()?.lowercase()
        if (contentType != null && contentType.isNotEmpty() &&
            !contentType.contains("mpegurl") && contentType != "text/plain"
        ) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("Response is not an HLS playlist")
            )
        }

        val finalUrl = response.finalUrl?.trim()
        if (finalUrl != null && !isHttpUrl(finalUrl)) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("Manifest redirect destination must be an absolute HTTP or HTTPS URL")
            )
        }

        return parser.parse(finalUrl ?: url, response.body)
    }

    private fun isHttpUrl(value: String): Boolean {
        val hasHttpScheme = value.startsWith("http://", true) ||
            value.startsWith("https://", true)
        if (!hasHttpScheme) return false
        val authority = value.substringAfter("://", "")
            .substringBefore('/').substringBefore('?').substringBefore('#')
        return authority.isNotBlank() && !authority.any(Char::isWhitespace)
    }

    private companion object {
        const val DEFAULT_MAX_RESPONSE_CHARS = 1_000_000
        const val MAX_ALLOWED_RESPONSE_CHARS = 5_000_000
        const val DEFAULT_TIMEOUT_MILLIS = 15_000L
    }
}
