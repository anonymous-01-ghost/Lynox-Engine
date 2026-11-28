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
    val maxHeight: Int? = null,
    val capabilities: MediaPlaybackCapabilities? = null,
    val preferredLanguage: String? = null,
    val preferAutoselect: Boolean = false
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
                    (stream.bitrate != null && stream.bitrate <= request.maxBitrate)) &&
                (request.maxWidth == null ||
                    (stream.width != null && stream.width <= request.maxWidth)) &&
                (request.maxHeight == null ||
                    (stream.height != null && stream.height <= request.maxHeight)) &&
                matchesCapabilities(stream, request.capabilities)
        }

        if (compatible.isEmpty()) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("No media stream matches the requested constraints")
            )
        }

        val selected = when (request.strategy) {
            MediaSelectionStrategy.BEST_QUALITY ->
                compatible.maxWithOrNull(
                    compareBy<MediaStream> { languageScore(it, request.preferredLanguage) }
                        .thenBy { autoselectScore(it, request.preferAutoselect) }
                        .thenBy { primaryQualityScore(it, request.kind) }
                        .thenBy { secondaryQualityScore(it, request.kind) }
                        .thenBy { it.isDefault }
                )

            MediaSelectionStrategy.LOWEST_BANDWIDTH ->
                compatible.minWithOrNull(
                    compareBy<MediaStream> { languageScore(it, request.preferredLanguage) * -1 }
                        .thenBy { autoselectScore(it, request.preferAutoselect) * -1 }
                        .thenBy { it.bitrate ?: Long.MAX_VALUE }
                        .thenByDescending { it.isDefault }
                )

            MediaSelectionStrategy.PREFER_DEFAULT ->
                compatible.maxWithOrNull(
                    compareBy<MediaStream> { it.isDefault }
                        .thenBy { languageScore(it, request.preferredLanguage) }
                        .thenBy { autoselectScore(it, request.preferAutoselect) }
                        .thenBy { primaryQualityScore(it, request.kind) }
                        .thenBy { secondaryQualityScore(it, request.kind) }
                )
        }

        return selected?.let { EngineResult.Success(it) }
            ?: EngineResult.Failure(EngineError.Unknown)
    }

    private fun languageScore(stream: MediaStream, preferredLanguage: String?): Int {
        val actual = stream.language?.trim()?.replace('_', '-') ?: return 0
        val preferred = preferredLanguage?.trim()?.replace('_', '-') ?: return 0
        if (actual.equals(preferred, ignoreCase = true)) return 2
        val actualBase = actual.substringBefore('-')
        val preferredBase = preferred.substringBefore('-')
        return if (actualBase.equals(preferredBase, ignoreCase = true)) 1 else 0
    }

    private fun autoselectScore(stream: MediaStream, preferAutoselect: Boolean): Int =
        if (preferAutoselect && stream.isAutoselect) 1 else 0

    private fun matchesCapabilities(
        stream: MediaStream,
        capabilities: MediaPlaybackCapabilities?
    ): Boolean {
        if (capabilities == null) return true

        val supportsKind = when (stream.kind) {
            MediaStreamKind.AUDIO -> capabilities.supportsAudio
            MediaStreamKind.VIDEO -> capabilities.supportsVideo
            MediaStreamKind.AUDIO_VIDEO -> capabilities.supportsAudio && capabilities.supportsVideo
        }
        if (!supportsKind) return false

        if (capabilities.supportedMimeTypes.isNotEmpty()) {
            val mime = stream.mimeType.substringBefore(';').trim().lowercase()
            val mimeMatches = capabilities.supportedMimeTypes.any { supported ->
                val normalized = supported.substringBefore(';').trim().lowercase()
                normalized == mime || (normalized.endsWith("/*") &&
                    mime.startsWith(normalized.removeSuffix("*") ))
            }
            if (!mimeMatches) return false
        }

        if (capabilities.supportedCodecs.isNotEmpty()) {
            val streamCodecs = stream.codecs
                ?.split(',')
                ?.map { it.trim().lowercase() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            if (streamCodecs.isEmpty() || streamCodecs.any { it !in capabilities.supportedCodecs.map(String::lowercase) }) {
                return false
            }
        }

        val bitrateLimits = when (stream.kind) {
            MediaStreamKind.AUDIO -> listOfNotNull(capabilities.maxAudioBitrate)
            MediaStreamKind.VIDEO -> listOfNotNull(capabilities.maxVideoBitrate)
            MediaStreamKind.AUDIO_VIDEO -> listOfNotNull(
                capabilities.maxAudioBitrate,
                capabilities.maxVideoBitrate
            )
        }
        val bitrateLimit = bitrateLimits.minOrNull()
        if (bitrateLimit != null && (stream.bitrate == null || stream.bitrate > bitrateLimit)) {
            return false
        }
        if (capabilities.maxWidth != null &&
            (stream.width == null || stream.width > capabilities.maxWidth)) return false
        if (capabilities.maxHeight != null &&
            (stream.height == null || stream.height > capabilities.maxHeight)) return false
        return true
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

    private fun primaryQualityScore(
        stream: MediaStream,
        requested: MediaSelectionKind
    ): Long = when (requested) {
        MediaSelectionKind.AUDIO -> stream.bitrate ?: -1L
        MediaSelectionKind.VIDEO,
        MediaSelectionKind.AUDIO_OR_VIDEO ->
            (stream.width?.toLong() ?: 0L) * (stream.height?.toLong() ?: 0L)
    }

    private fun secondaryQualityScore(
        stream: MediaStream,
        requested: MediaSelectionKind
    ): Long = when (requested) {
        MediaSelectionKind.AUDIO -> 0L
        MediaSelectionKind.VIDEO,
        MediaSelectionKind.AUDIO_OR_VIDEO -> stream.bitrate ?: -1L
    }
}
