package dev.lynox.engine.extractor

import kotlinx.serialization.Serializable

/** Describes one media rendition returned by a configured source resolver. */
@Serializable
data class MediaStream(
    val url: String,
    val kind: MediaStreamKind,
    val mimeType: String,
    val codecs: String? = null,
    val bitrate: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Float? = null,
    val contentLength: Long? = null,
    val isDefault: Boolean = false,
    val language: String? = null,
    val label: String? = null,
    val isAutoselect: Boolean = false
)

/** Media track category. */
@Serializable
enum class MediaStreamKind {
    AUDIO,
    VIDEO,
    AUDIO_VIDEO
}

/**
 * Resolved media information. Stream URLs may be short-lived and must not be
 * persisted or cached beyond the source's validity window.
 */
@Serializable
data class ResolvedMedia(
    val sourceUrl: String,
    val title: String? = null,
    val durationSeconds: Long? = null,
    val streams: List<MediaStream>,
    val expiresAtEpochSeconds: Long? = null,
    val subtitles: List<MediaSubtitleTrack> = emptyList()
) {
    init {
        require(sourceUrl.isNotBlank()) { "sourceUrl cannot be blank" }
        require(streams.isNotEmpty()) { "At least one media stream is required" }
    }
}


/** A subtitle rendition declared by an HLS master playlist. */
@Serializable
data class MediaSubtitleTrack(
    val url: String,
    val language: String? = null,
    val label: String? = null,
    val isDefault: Boolean = false,
    val isAutoselect: Boolean = false,
    val isForced: Boolean = false
)
