package dev.lynox.engine.extractor

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android transport using HttpURLConnection.
 *
 * Response bytes are bounded while being read. Callers should still configure
 * Android network security and cleartext policy in the consuming application.
 */
class AndroidManifestHttpClient : ManifestHttpClient {
    override suspend fun get(request: ManifestHttpRequest): ManifestHttpResponse =
        withContext(Dispatchers.IO) {
            require(request.maxResponseChars > 0) { "maxResponseChars must be positive" }
            require(request.timeoutMillis > 0) { "timeoutMillis must be positive" }

            var connection: HttpURLConnection? = null
            try {
                connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = request.timeoutMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    readTimeout = request.timeoutMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/vnd.apple.mpegurl, audio/mpegurl, text/plain")
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > request.maxResponseChars * 4L) {
                            throw IllegalStateException("Manifest response exceeds configured byte limit")
                        }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray().toString(Charsets.UTF_8)
                }.orEmpty()

                ManifestHttpResponse(
                    statusCode = status,
                    body = body,
                    contentType = connection.contentType,
                    finalUrl = connection.url.toString()
                )
            } finally {
                connection?.disconnect()
            }
        }
}
