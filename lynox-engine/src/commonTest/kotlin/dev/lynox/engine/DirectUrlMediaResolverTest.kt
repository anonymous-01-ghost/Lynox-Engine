package dev.lynox.engine

import dev.lynox.engine.extractor.DirectUrlMediaResolver
import dev.lynox.engine.extractor.MediaStreamKind
import dev.lynox.engine.extractor.ResolvedMedia
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class DirectUrlMediaResolverTest {

    @Test
    fun resolvesDirectAudioFileAndIgnoresQueryParameters() = runTest {
        val result = assertIs<EngineResult.Success<ResolvedMedia>>(
            DirectUrlMediaResolver().resolve(" https://cdn.example.test/music/song.m4a?token=temporary ")
        )

        assertEquals("https://cdn.example.test/music/song.m4a?token=temporary", result.value.sourceUrl)
        assertEquals("audio/mp4", result.value.streams.single().mimeType)
        assertEquals(MediaStreamKind.AUDIO, result.value.streams.single().kind)
        assertEquals(true, result.value.streams.single().isDefault)
    }

    @Test
    fun resolvesDirectVideoFile() = runTest {
        val result = assertIs<EngineResult.Success<ResolvedMedia>>(
            DirectUrlMediaResolver().resolve("https://cdn.example.test/video.webm")
        )

        assertEquals("video/webm", result.value.streams.single().mimeType)
        assertEquals(MediaStreamKind.AUDIO_VIDEO, result.value.streams.single().kind)
    }

    @Test
    fun rejectsWatchPagesAndUnknownExtensions() = runTest {
        val result = assertIs<EngineResult.Failure>(
            DirectUrlMediaResolver().resolve("https://media.example.test/watch?id=123")
        )

        val error = assertIs<EngineError.InvalidRequest>(result.error)
        assertEquals(
            "URL does not identify a supported direct media file",
            error.reason
        )
    }

    @Test
    fun rejectsNonHttpUrls() = runTest {
        val result = assertIs<EngineResult.Failure>(
            DirectUrlMediaResolver().resolve("file:///music/song.mp3")
        )

        assertIs<EngineError.InvalidRequest>(result.error)
    }
}
