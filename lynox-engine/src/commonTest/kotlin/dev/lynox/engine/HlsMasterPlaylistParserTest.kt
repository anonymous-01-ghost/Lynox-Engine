package dev.lynox.engine

import dev.lynox.engine.extractor.HlsMasterPlaylistParser
import dev.lynox.engine.extractor.MediaStreamKind
import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HlsMasterPlaylistParserTest {
    private val parser = HlsMasterPlaylistParser()

    @Test
    fun parsesVariantMetadataAndRelativeUrls() {
        val manifest = """#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=1280000,AVERAGE-BANDWIDTH=1100000,RESOLUTION=1280x720,FRAME-RATE=30.0,CODECS="avc1.64001f,mp4a.40.2"
video/720/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=500000,RESOLUTION=640x360,CODECS="avc1.4d401e"
../360/index.m3u8
"""
        val result = assertIs<EngineResult.Success<*>>(
            parser.parse("https://cdn.example.test/hls/master.m3u8", manifest)
        )
        val streams = result.value.let { (it as dev.lynox.engine.extractor.ResolvedMedia).streams }
        assertEquals(2, streams.size)
        assertEquals("https://cdn.example.test/hls/video/720/index.m3u8", streams[0].url)
        assertEquals(1_100_000L, streams[0].bitrate)
        assertEquals(1280, streams[0].width)
        assertEquals(720, streams[0].height)
        assertEquals("avc1.64001f,mp4a.40.2", streams[0].codecs)
        assertEquals("https://cdn.example.test/360/index.m3u8", streams[1].url)
    }

    @Test
    fun parsesSeparateAudioRendition() {
        val manifest = """#EXTM3U
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",DEFAULT=YES,AUTOSELECT=YES,URI="audio/en.m3u8"
#EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1920x1080,AUDIO="audio",CODECS="avc1.640028,mp4a.40.2"
video/1080.m3u8
"""
        val result = assertIs<EngineResult.Success<*>>(
            parser.parse("https://cdn.example.test/master.m3u8", manifest)
        )
        val streams = (result.value as dev.lynox.engine.extractor.ResolvedMedia).streams
        assertEquals(2, streams.size)
        assertEquals(MediaStreamKind.VIDEO, streams[0].kind)
        assertEquals("https://cdn.example.test/audio/en.m3u8", streams[1].url)
        assertEquals(MediaStreamKind.AUDIO, streams[1].kind)
        assertTrue(streams[1].isDefault)
    }

    @Test
    fun preservesAllAudioRenditionsInReferencedGroup() {
        val manifest = """#EXTM3U
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",LANGUAGE="en-US",DEFAULT=YES,AUTOSELECT=YES,URI="audio/en.m3u8"
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Spanish",LANGUAGE="es",DEFAULT=NO,URI="audio/es.m3u8"
#EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1920x1080,AUDIO="audio",CODECS="avc1.640028,mp4a.40.2"
video/1080.m3u8
"""
        val result = assertIs<EngineResult.Success<*>>(
            parser.parse("https://cdn.example.test/master.m3u8", manifest)
        )
        val streams = (result.value as dev.lynox.engine.extractor.ResolvedMedia).streams
        val audio = streams.filter { it.kind == MediaStreamKind.AUDIO }

        assertEquals(2, audio.size)
        assertEquals(
            setOf(
                "https://cdn.example.test/audio/en.m3u8",
                "https://cdn.example.test/audio/es.m3u8"
            ),
            audio.map { it.url }.toSet()
        )
        assertEquals(1, audio.count { it.isDefault })
        val english = audio.single { it.language == "en-US" }
        assertEquals("English", english.label)
        assertTrue(english.isAutoselect)
        assertEquals("es", audio.single { it.url.endsWith("es.m3u8") }.language)
    }

    @Test
    fun resolvesAudioGroupDeclaredAfterItsVariant() {
        val manifest = """#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720,AUDIO="audio",CODECS="avc1.64001f,mp4a.40.2"
video/720.m3u8
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",DEFAULT=YES,URI="audio/en.m3u8"
"""
        val result = assertIs<EngineResult.Success<*>>(
            parser.parse("https://cdn.example.test/master.m3u8", manifest)
        )
        val streams = (result.value as dev.lynox.engine.extractor.ResolvedMedia).streams

        assertEquals(2, streams.size)
        assertEquals(MediaStreamKind.VIDEO, streams[0].kind)
        assertEquals(MediaStreamKind.AUDIO, streams[1].kind)
        assertEquals("https://cdn.example.test/audio/en.m3u8", streams[1].url)
    }


    @Test
    fun parsesReferencedSubtitleRenditionsAndMetadata() {
        val manifest = """#EXTM3U
#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="Español",LANGUAGE="es",DEFAULT=YES,AUTOSELECT=YES,FORCED=NO,URI="subs/es.m3u8"
#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="English",LANGUAGE="en",DEFAULT=NO,AUTOSELECT=NO,FORCED=YES,URI="../subs/en.m3u8"
#EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=1280x720,SUBTITLES="subs"
video/720.m3u8
"""
        val result = assertIs<EngineResult.Success<*>>(
            parser.parse("https://cdn.example.test/hls/master.m3u8", manifest)
        )
        val resolved = result.value as dev.lynox.engine.extractor.ResolvedMedia

        assertEquals(2, resolved.subtitles.size)
        val spanish = resolved.subtitles.single { it.language == "es" }
        assertEquals("Español", spanish.label)
        assertEquals("https://cdn.example.test/hls/subs/es.m3u8", spanish.url)
        assertTrue(spanish.isDefault)
        assertTrue(spanish.isAutoselect)
        val english = resolved.subtitles.single { it.language == "en" }
        assertEquals("https://cdn.example.test/subs/en.m3u8", english.url)
        assertTrue(english.isForced)
    }

    @Test
    fun ignoresUnreferencedSubtitleGroups() {
        val manifest = """#EXTM3U
#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="unused",NAME="Español",URI="subs/es.m3u8"
#EXT-X-STREAM-INF:BANDWIDTH=1000000
video.m3u8
"""
        val result = assertIs<EngineResult.Success<*>>(
            parser.parse("https://cdn.example.test/master.m3u8", manifest)
        )
        val resolved = result.value as dev.lynox.engine.extractor.ResolvedMedia
        assertTrue(resolved.subtitles.isEmpty())
    }

    @Test
    fun resolvesQueryOnlyAndRelativeReferencesWithFragments() {
        val queryResult = assertIs<EngineResult.Success<*>>(
            parser.parse(
                "https://cdn.example.test/hls/master.m3u8?session=old",
                "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1000\n?session=new"
            )
        )
        val queryStream = (queryResult.value as dev.lynox.engine.extractor.ResolvedMedia).streams.single()
        assertEquals("https://cdn.example.test/hls/master.m3u8?session=new", queryStream.url)

        val fragmentResult = assertIs<EngineResult.Success<*>>(
            parser.parse(
                "https://cdn.example.test/hls/master.m3u8?session=old",
                "#EXTM3U\\n#EXT-X-STREAM-INF:BANDWIDTH=1000\\nvideo/variant.m3u8#variant"
            )
        )
        val fragmentStream = (fragmentResult.value as dev.lynox.engine.extractor.ResolvedMedia).streams.single()
        assertEquals("https://cdn.example.test/hls/video/variant.m3u8#variant", fragmentStream.url)
    }

    @Test
    fun rejectsInvalidHeaderAndManifestWithoutVariants() {
        val badHeader = assertIs<EngineResult.Failure>(
            parser.parse("https://cdn.example.test/master.m3u8", "not a playlist")
        )
        assertIs<EngineError.InvalidRequest>(badHeader.error)

        val noVariants = assertIs<EngineResult.Failure>(
            parser.parse("https://cdn.example.test/master.m3u8", "#EXTM3U\n#EXT-X-VERSION:3")
        )
        assertIs<EngineError.InvalidRequest>(noVariants.error)
    }

    @Test
    fun rejectsNonHttpManifestUrl() {
        val result = assertIs<EngineResult.Failure>(
            parser.parse("file:///tmp/master.m3u8", "#EXTM3U")
        )
        assertIs<EngineError.InvalidRequest>(result.error)
    }
}
