package dev.lynox.engine

import dev.lynox.engine.extractor.MediaPlaybackCapabilities
import dev.lynox.engine.extractor.StaticMediaPlaybackCapabilityProvider
import dev.lynox.engine.extractor.MediaSelectionKind
import dev.lynox.engine.extractor.MediaSelectionRequest
import dev.lynox.engine.extractor.MediaSelectionStrategy
import dev.lynox.engine.extractor.MediaStream
import dev.lynox.engine.extractor.MediaStreamKind
import dev.lynox.engine.extractor.MediaStreamSelector
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class MediaStreamSelectorTest {
    private val selector = MediaStreamSelector()

    @Test
    fun selectsHighestBitrateAudioByDefault() {
        val low = audio("https://cdn.test/low.m4a", 96_000)
        val high = audio("https://cdn.test/high.m4a", 256_000)

        val result = assertIs<EngineResult.Success<MediaStream>>(
            selector.select(listOf(low, high))
        )

        assertEquals(high.url, result.value.url)
    }

    @Test
    fun selectsLowestBandwidthWhenRequested() {
        val low = audio("https://cdn.test/low.m4a", 64_000)
        val high = audio("https://cdn.test/high.m4a", 192_000)

        val result = assertIs<EngineResult.Success<MediaStream>>(
            selector.select(
                listOf(high, low),
                MediaSelectionRequest(strategy = MediaSelectionStrategy.LOWEST_BANDWIDTH)
            )
        )

        assertEquals(low.url, result.value.url)
    }

    @Test
    fun filtersByRequestedKindAndDimensions() {
        val audioOnly = audio("https://cdn.test/audio.m4a", 256_000)
        val smallVideo = video("https://cdn.test/720.mp4", 1280, 720, 1_500_000)
        val largeVideo = video("https://cdn.test/1080.mp4", 1920, 1080, 3_000_000)

        val result = assertIs<EngineResult.Success<MediaStream>>(
            selector.select(
                listOf(audioOnly, smallVideo, largeVideo),
                MediaSelectionRequest(
                    kind = MediaSelectionKind.VIDEO,
                    maxWidth = 1280,
                    maxHeight = 720
                )
            )
        )

        assertEquals(smallVideo.url, result.value.url)
    }

    @Test
    fun enforcesMaximumBitrateAndExcludesUnknownBitrate() {
        val unknown = MediaStream(
            url = "https://cdn.test/unknown.m4a",
            kind = MediaStreamKind.AUDIO,
            mimeType = "audio/mp4"
        )
        val tooHigh = audio("https://cdn.test/high.m4a", 320_000)
        val withinLimit = audio("https://cdn.test/within.m4a", 128_000)

        val result = assertIs<EngineResult.Success<MediaStream>>(
            selector.select(
                listOf(unknown, tooHigh, withinLimit),
                MediaSelectionRequest(maxBitrate = 160_000)
            )
        )

        assertEquals(withinLimit.url, result.value.url)
    }

    @Test
    fun returnsFailureWhenNoRenditionMatches() {
        val result = assertIs<EngineResult.Failure>(
            selector.select(
                listOf(audio("https://cdn.test/audio.m4a", 256_000)),
                MediaSelectionRequest(kind = MediaSelectionKind.VIDEO)
            )
        )

        assertIs<EngineError.InvalidRequest>(result.error)
    }

    @Test
    fun prefersDefaultWhenRequested() {
        val default = audio("https://cdn.test/default.m4a", 96_000, isDefault = true)
        val higher = audio("https://cdn.test/high.m4a", 256_000)

        val result = assertIs<EngineResult.Success<MediaStream>>(
            selector.select(
                listOf(higher, default),
                MediaSelectionRequest(strategy = MediaSelectionStrategy.PREFER_DEFAULT)
            )
        )

        assertEquals(default.url, result.value.url)
    }


    @Test
    fun filtersByPlaybackMimeCodecAndDeviceLimits() {
        val unsupportedCodec = MediaStream(
            url = "https://cdn.test/av1.mp4",
            kind = MediaStreamKind.VIDEO,
            mimeType = "video/mp4",
            codecs = "av01.0.08M.08",
            bitrate = 2_000_000,
            width = 1920,
            height = 1080
        )
        val tooLarge = video("https://cdn.test/1080-h264.mp4", 1920, 1080, 2_000_000).copy(
            codecs = "avc1.640028"
        )
        val supported = video("https://cdn.test/720-h264.mp4", 1280, 720, 1_200_000).copy(
            codecs = "avc1.4d401f"
        )

        val result = assertIs<EngineResult.Success<MediaStream>>(
            selector.select(
                listOf(unsupportedCodec, tooLarge, supported),
                MediaSelectionRequest(
                    kind = MediaSelectionKind.VIDEO,
                    capabilities = MediaPlaybackCapabilities(
                        supportedMimeTypes = setOf("video/*"),
                        supportedCodecs = setOf("avc1.4d401f"),
                        maxVideoBitrate = 1_500_000,
                        maxWidth = 1280,
                        maxHeight = 720
                    )
                )
            )
        )

        assertEquals(supported.url, result.value.url)
    }

    @Test
    fun rejectsMediaKindNotSupportedByPlaybackClient() {
        val audioOnly = audio("https://cdn.test/audio.m4a", 128_000)
        val result = assertIs<EngineResult.Failure>(
            selector.select(
                listOf(audioOnly),
                MediaSelectionRequest(
                    capabilities = MediaPlaybackCapabilities(supportsAudio = false)
                )
            )
        )
        assertIs<EngineError.InvalidRequest>(result.error)
    }


    @Test
    fun staticCapabilityProviderReturnsConfiguredProfile() = runBlocking {
        val profile = MediaPlaybackCapabilities(
            supportedMimeTypes = setOf("audio/mp4"),
            supportedCodecs = setOf("mp4a.40.2"),
            maxAudioBitrate = 256_000
        )
        val provider = StaticMediaPlaybackCapabilityProvider(profile)

        assertEquals(profile, provider.getCapabilities())
    }

    private fun audio(url: String, bitrate: Long, isDefault: Boolean = false) =
        MediaStream(
            url = url,
            kind = MediaStreamKind.AUDIO,
            mimeType = "audio/mp4",
            bitrate = bitrate,
            isDefault = isDefault
        )

    private fun video(url: String, width: Int, height: Int, bitrate: Long) =
        MediaStream(
            url = url,
            kind = MediaStreamKind.VIDEO,
            mimeType = "video/mp4",
            width = width,
            height = height,
            bitrate = bitrate
        )
}
