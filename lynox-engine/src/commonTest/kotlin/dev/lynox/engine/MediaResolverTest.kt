package dev.lynox.engine

import dev.lynox.engine.extractor.MediaResolver
import dev.lynox.engine.extractor.MediaStream
import dev.lynox.engine.extractor.MediaStreamKind
import dev.lynox.engine.extractor.ResolvedMedia
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class MediaResolverTest {
    @Test
    fun resolverReceivesNormalizedHttpUrlAndReturnsStreams() = runTest {
        var receivedUrl: String? = null
        val resolver = object : MediaResolver {
            override suspend fun resolve(sourceUrl: String): EngineResult<ResolvedMedia> {
                receivedUrl = sourceUrl
                return EngineResult.Success(
                    ResolvedMedia(
                        sourceUrl = sourceUrl,
                        title = "Sample",
                        streams = listOf(
                            MediaStream(
                                url = "https://cdn.example.test/audio.m4a",
                                kind = MediaStreamKind.AUDIO,
                                mimeType = "audio/mp4"
                            )
                        )
                    )
                )
            }
        }
        val engine = LynoxEngine(mediaResolver = resolver)

        val result = assertIs<EngineResult.Success<ResolvedMedia>>(
            engine.resolveMedia("  https://media.example.test/watch?id=1  ")
        )

        assertEquals("https://media.example.test/watch?id=1", receivedUrl)
        assertEquals("audio/mp4", result.value.streams.single().mimeType)
    }

    @Test
    fun resolverRejectsNonHttpSourceUrls() = runTest {
        val engine = LynoxEngine(mediaResolver = object : MediaResolver {
            override suspend fun resolve(sourceUrl: String) =
                EngineResult.Failure(EngineError.Unknown)
        })

        val result = assertIs<EngineResult.Failure>(engine.resolveMedia("file:///private/media.mp4"))
        assertIs<EngineError.InvalidRequest>(result.error)
    }

    @Test
    fun resolverRejectsHttpUrlsWithoutAHost() = runTest {
        val result = assertIs<EngineResult.Failure>(
            LynoxEngine(mediaResolver = object : MediaResolver {
                override suspend fun resolve(sourceUrl: String) =
                    EngineResult.Failure(EngineError.Unknown)
            }).resolveMedia("https://")
        )
        assertIs<EngineError.InvalidRequest>(result.error)
    }

    @Test
    fun resolverMustBeConfigured() = runTest {
        val result = assertIs<EngineResult.Failure>(
            LynoxEngine().resolveMedia("https://media.example.test/watch")
        )
        assertIs<EngineError.ProviderNotConfigured>(result.error)
    }
}
