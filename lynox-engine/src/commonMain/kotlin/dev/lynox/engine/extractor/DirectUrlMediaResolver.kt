package dev.lynox.engine.extractor

import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult

/**
 * Resolves a publicly accessible direct media-file URL without a server or
 * network request. This adapter does not discover pages, playlists, or streams
 * inside third-party services.
 *
 * Only extensions with an unambiguous or broadly supported media type are
 * accepted. HLS/DASH manifests and service watch-page URLs are intentionally
 * not treated as direct media files.
 */
class DirectUrlMediaResolver : MediaResolver {

    override suspend fun resolve(sourceUrl: String): EngineResult<ResolvedMedia> {
        val url = sourceUrl.trim()
        if (!isHttpUrl(url)) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("sourceUrl must be an absolute HTTP or HTTPS URL")
            )
        }

        val path = url.substringBefore('#').substringBefore('?')
        val fileName = path.substringAfterLast('/')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()

        val mediaType = MEDIA_TYPES[extension]
            ?: return EngineResult.Failure(
                EngineError.InvalidRequest("URL does not identify a supported direct media file")
            )

        return EngineResult.Success(
            ResolvedMedia(
                sourceUrl = url,
                streams = listOf(
                    MediaStream(
                        url = url,
                        kind = mediaType.kind,
                        mimeType = mediaType.mimeType,
                        isDefault = true
                    )
                )
            )
        )
    }

    private fun isHttpUrl(url: String): Boolean {
        val isHttp = url.startsWith("https://", ignoreCase = true) ||
            url.startsWith("http://", ignoreCase = true)
        if (!isHttp) return false
        val authority = url.substringAfter("://", "")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
        return authority.isNotBlank() && !authority.any(Char::isWhitespace)
    }

    private data class MediaType(
        val mimeType: String,
        val kind: MediaStreamKind
    )

    private companion object {
        val AUDIO = MediaStreamKind.AUDIO
        val AUDIO_VIDEO = MediaStreamKind.AUDIO_VIDEO

        val MEDIA_TYPES = mapOf(
            "mp3" to MediaType("audio/mpeg", AUDIO),
            "m4a" to MediaType("audio/mp4", AUDIO),
            "aac" to MediaType("audio/aac", AUDIO),
            "ogg" to MediaType("audio/ogg", AUDIO),
            "oga" to MediaType("audio/ogg", AUDIO),
            "opus" to MediaType("audio/opus", AUDIO),
            "wav" to MediaType("audio/wav", AUDIO),
            "flac" to MediaType("audio/flac", AUDIO),
            "mp4" to MediaType("video/mp4", AUDIO_VIDEO),
            "m4v" to MediaType("video/x-m4v", AUDIO_VIDEO),
            "webm" to MediaType("video/webm", AUDIO_VIDEO),
            "mov" to MediaType("video/quicktime", AUDIO_VIDEO),
            "mkv" to MediaType("video/x-matroska", AUDIO_VIDEO)
        )
    }
}
