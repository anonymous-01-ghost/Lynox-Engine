package dev.lynox.engine.extractor

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class JvmManifestHttpClientTest {

    @Test
    fun fetchesManifestAndFollowsRedirects() = runTest {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/master.m3u8") { exchange ->
            exchange.responseHeaders.add("Location", "/final/master.m3u8")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/final/master.m3u8") { exchange ->
            val body = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1280000\nvideo.m3u8"
                .toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/vnd.apple.mpegurl")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val response = JvmManifestHttpClient().get(
                ManifestHttpRequest(
                    url = "$baseUrl/master.m3u8",
                    maxResponseChars = 1_000,
                    timeoutMillis = 2_000
                )
            )

            assertEquals(200, response.statusCode)
            assertEquals("application/vnd.apple.mpegurl", response.contentType)
            assertEquals("$baseUrl/final/master.m3u8", response.finalUrl)
            assertEquals(true, response.body.startsWith("#EXTM3U"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun rejectsResponseLargerThanConfiguredByteCeiling() = runTest {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/large") { exchange ->
            val body = "x".repeat(64).toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            val client = JvmManifestHttpClient()
            assertFailsWith<IllegalStateException> {
                client.get(
                    ManifestHttpRequest(
                        url = "http://127.0.0.1:${server.address.port}/large",
                        maxResponseChars = 4,
                        timeoutMillis = 2_000
                    )
                )
            }
        } finally {
            server.stop(0)
        }
    }
}
