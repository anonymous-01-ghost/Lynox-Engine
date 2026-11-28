package dev.lynox.engine

import dev.lynox.engine.extractor.HlsManifestResolver
import dev.lynox.engine.extractor.ManifestHttpClient
import dev.lynox.engine.extractor.ManifestHttpRequest
import dev.lynox.engine.extractor.ManifestHttpResponse
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class HlsManifestResolverTest {

    private val playlist = """
        #EXTM3U
        #EXT-X-STREAM-INF:BANDWIDTH=1280000,RESOLUTION=1280x720
        video/720.m3u8
    """.trimIndent()

    @Test
    fun fetchesAndParsesManifestUsingInjectedTransport() = runTest {
        var received: ManifestHttpRequest? = null
        val resolver = HlsManifestResolver(object : ManifestHttpClient {
            override suspend fun get(request: ManifestHttpRequest): ManifestHttpResponse {
                received = request
                return ManifestHttpResponse(
                    statusCode = 200,
                    body = playlist,
                    contentType = "application/vnd.apple.mpegurl",
                    finalUrl = "https://cdn.example.test/path/master.m3u8"
                )
            }
        })

        val result = assertIs<EngineResult.Success<dev.lynox.engine.extractor.ResolvedMedia>>(
            resolver.resolve("https://origin.example.test/master.m3u8")
        )

        assertEquals(1_000_000, received?.maxResponseChars)
        assertEquals(15_000L, received?.timeoutMillis)
        assertEquals(
            "https://cdn.example.test/path/video/720.m3u8",
            result.value.streams.single().url
        )
    }

    @Test
    fun rejectsUnauthorizedResponse() = runTest {
        val resolver = HlsManifestResolver(object : ManifestHttpClient {
            override suspend fun get(request: ManifestHttpRequest) =
                ManifestHttpResponse(403, "forbidden")
        })

        val result = assertIs<EngineResult.Failure>(
            resolver.resolve("https://media.example.test/master.m3u8")
        )
        assertIs<EngineError.Unauthorized>(result.error)
    }

    @Test
    fun rejectsServerErrorAsNetworkFailure() = runTest {
        val resolver = HlsManifestResolver(object : ManifestHttpClient {
            override suspend fun get(request: ManifestHttpRequest) =
                ManifestHttpResponse(503, "unavailable")
        })

        val result = assertIs<EngineResult.Failure>(
            resolver.resolve("https://media.example.test/master.m3u8")
        )
        assertIs<EngineError.Network>(result.error)
    }

    @Test
    fun rejectsOversizedResponse() = runTest {
        val resolver = HlsManifestResolver(
            httpClient = object : ManifestHttpClient {
                override suspend fun get(request: ManifestHttpRequest) =
                    ManifestHttpResponse(200, "x".repeat(101))
            },
            maxResponseChars = 100
        )

        val result = assertIs<EngineResult.Failure>(
            resolver.resolve("https://media.example.test/master.m3u8")
        )
        assertIs<EngineError.InvalidRequest>(result.error)
    }

    @Test
    fun rejectsNonPlaylistContentType() = runTest {
        val resolver = HlsManifestResolver(object : ManifestHttpClient {
            override suspend fun get(request: ManifestHttpRequest) =
                ManifestHttpResponse(200, playlist, "application/json")
        })

        val result = assertIs<EngineResult.Failure>(
            resolver.resolve("https://media.example.test/master.m3u8")
        )
        assertIs<EngineError.InvalidRequest>(result.error)
    }

    @Test
    fun mapsTransportExceptionsToNetworkErrors() = runTest {
        val resolver = HlsManifestResolver(object : ManifestHttpClient {
            override suspend fun get(request: ManifestHttpRequest): ManifestHttpResponse {
                error("offline")
            }
        })

        val result = assertIs<EngineResult.Failure>(
            resolver.resolve("https://media.example.test/master.m3u8")
        )
        assertIs<EngineError.Network>(result.error)
    }

    @Test
    fun rejectsNonHttpRedirectDestination() = runTest {
        val resolver = HlsManifestResolver(object : ManifestHttpClient {
            override suspend fun get(request: ManifestHttpRequest) =
                ManifestHttpResponse(
                    statusCode = 200,
                    body = playlist,
                    contentType = "application/vnd.apple.mpegurl",
                    finalUrl = "file:///private/manifest.m3u8"
                )
        })

        val result = assertIs<EngineResult.Failure>(
            resolver.resolve("https://media.example.test/master.m3u8")
        )
        assertIs<EngineError.InvalidRequest>(result.error)
    }

    @Test
    fun acceptsHttpRedirectDestinationAndUsesItAsBaseUrl() = runTest {
        val resolver = HlsManifestResolver(object : ManifestHttpClient {
            override suspend fun get(request: ManifestHttpRequest) =
                ManifestHttpResponse(
                    statusCode = 200,
                    body = playlist,
                    contentType = "application/vnd.apple.mpegurl",
                    finalUrl = "https://cdn.example.test/redirected/master.m3u8"
                )
        })

        val result = assertIs<EngineResult.Success<dev.lynox.engine.extractor.ResolvedMedia>>(
            resolver.resolve("https://media.example.test/master.m3u8")
        )
        assertEquals(
            "https://cdn.example.test/redirected/video/720.m3u8",
            result.value.streams.single().url
        )
    }
}
