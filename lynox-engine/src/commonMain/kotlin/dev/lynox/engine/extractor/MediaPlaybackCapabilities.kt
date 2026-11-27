package dev.lynox.engine.extractor

/**
 * Declares formats and limits accepted by a playback client.
 *
 * Empty MIME/codec sets mean "not restricted by this declaration". This is
 * descriptive capability data; platform adapters must populate it from the
 * actual player/device rather than relying on assumed platform support.
 */
data class MediaPlaybackCapabilities(
    val supportedMimeTypes: Set<String> = emptySet(),
    val supportedCodecs: Set<String> = emptySet(),
    val supportsAudio: Boolean = true,
    val supportsVideo: Boolean = true,
    val maxAudioBitrate: Long? = null,
    val maxVideoBitrate: Long? = null,
    val maxWidth: Int? = null,
    val maxHeight: Int? = null
) {
    init {
        require(maxAudioBitrate == null || maxAudioBitrate > 0) {
            "maxAudioBitrate must be positive"
        }
        require(maxVideoBitrate == null || maxVideoBitrate > 0) {
            "maxVideoBitrate must be positive"
        }
        require(maxWidth == null || maxWidth > 0) { "maxWidth must be positive" }
        require(maxHeight == null || maxHeight > 0) { "maxHeight must be positive" }
    }
}
