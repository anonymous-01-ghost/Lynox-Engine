package dev.lynox.engine.extractor

import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult

/** Requested media category for rendition selection. */
enum class MediaSelectionKind {
    AUDIO,
    VIDEO,
    AUDIO_OR_VIDEO
}

/** Strategy used to rank compatible renditions. */
enum class MediaSelectionStrategy {
    BEST_QUALITY,
    LOWEST_BANDWIDTH,
    PREFER_DEFAULT
}

/**
 * Constraints applied before ranking available renditions.
 *
 * A null limit means that the corresponding property is unrestricted.
 */
data class MediaSelectionRequest(
    val kind: MediaSelectionKind = MediaSelectionKind.AUDIO,
    val strategy: MediaSelectionStrategy = MediaSelectionStrategy.BEST_QUALITY,
    val maxBitrate: Long? = null,
    val maxWidth: Int? = null,
    val maxHeight: Int? = null
) {
    init {
        require(maxBitrate == null || maxBitrate > 0) {
            "maxBitrate must be positive"
        }
        require(maxWidth == null || maxWidth > 0) {
            "maxWidth must be positive"
        }
        require(maxHeight == null || maxHeight > 0) {
            "maxHeight must be positive"
        }
    }
}

/**
 * Selects one rendition from an already resolved list.
 *
 * This class does not fetch URLs, inspect remote content, or bypass source
 * restrictions. It only ranks metadata supplied by a configured resolver.
 */
class MediaStreamSelector {

    fun select(
        streams: List<MediaStream>,
        request: MediaSelectionRequest = MediaSelectionRequest()
    ): EngineResult<MediaStream> {
        if (streams.isEmpty()) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("At least one media stream is required")
            )
        }

        val compatible = streams.filter { stream ->
            matchesKind(stream, request.kind) &&
                (request.maxBitrate == null ||
                    stream.bitrate == null ||
                    stream.bitrate <= request.maxBitrate) &&
                (request.maxWidth == null ||
                    stream.width == null ||
                    stream.width <= request.maxWidth) &&
                (request.maxHeight == null ||
                    stream.height == null ||
                    stream.height <= request.maxHeight)
        }

        if (compatible.isEmpty()) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("No media stream matches the requested constraints")
            )
        }

        val selected = when (request.strategy) {
            MediaSelectionStrategy.BEST_QUALITY ->
                compatible.maxWithOrNull(
                    compareBy<MediaStream> { qualityScore(it, request.kind) }
                        .thenBy { it.isDefault }
                )

            MediaSelectionStrategy.LOWEST_BANDWIDTH ->
                compatible.minWithOrNull(
                    compareBy<MediaStream> { it.bitrate ?: Long.MAX_VALUE }
                        .thenByDescending { it.isDefault }
                )

            MediaSelectionStrategy.PREFER_DEFAULT ->
                compatible.maxWithOrNull(
                    compareBy<MediaStream> { it.isDefault }
                        .thenBy { qualityScore(it, request.kind) }
                )
        }

        return selected?.let { EngineResult.Success(it) }
            ?: EngineResult.Failure(EngineError.Unknown)
    }

    private fun matchesKind(
        stream: MediaStream,
        requested: MediaSelectionKind
    ): Boolean = when (requested) {
        MediaSelectionKind.AUDIO ->
            stream.kind == MediaStreamKind.AUDIO ||
                stream.kind == MediaStreamKind.AUDIO_VIDEO

        MediaSelectionKind.VIDEO ->
            stream.kind == MediaStreamKind.VIDEO ||
                stream.kind == MediaStreamKind.AUDIO_VIDEO

        MediaSelectionKind.AUDIO_OR_VIDEO -> true
    }

    private fun qualityScore(
        stream: MediaStream,
        requested: MediaSelectionKind
    ): Long {
        val bitrate = stream.bitrate ?: -1L
        val pixels = (stream.width?.toLong() ?: 0L) *
            (stream.height?.toLong() ?: 0L)

        return when (requested) {
            MediaSelectionKind.AUDIO -> bitrate
            MediaSelectionKind.VIDEO,
            MediaSelectionKind.AUDIO_OR_VIDEO -> {
                // Resolution is the primary video quality signal; bitrate breaks ties.
                val safePixels = pixels.coerceAtMost(1_000_000_000L)
                safePixels * 1_000L + bitrate.coerceAtLeast(-1L).coerceAtMost(999L)
            }
        }
    }
}
