package dev.lynox.engine.extractor

import android.media.MediaCodecList

/**
 * Discovers decoder MIME types and conservative aggregate limits from Android's
 * regular codec list.
 *
 * This reports platform decoder availability, not a guarantee that every
 * stream can be played by a particular Media3 renderer or device configuration.
 * Codec profile/level strings are intentionally not inferred from codec names.
 */
class AndroidMediaCodecCapabilityProvider : MediaPlaybackCapabilityProvider {

    override suspend fun getCapabilities(): MediaPlaybackCapabilities {
        val mimeTypes = linkedSetOf<String>()
        var maxAudioBitrate: Long? = null
        var maxVideoBitrate: Long? = null
        var maxWidth: Int? = null
        var maxHeight: Int? = null

        val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        codecInfos.asSequence()
            .filterNot { it.isEncoder }
            .forEach { codecInfo ->
                codecInfo.supportedTypes.forEach mimeTypeLoop@{ rawMimeType ->
                    val mimeType = rawMimeType.lowercase()
                    val codecCapabilities = runCatching {
                        codecInfo.getCapabilitiesForType(rawMimeType)
                    }.getOrNull() ?: return@mimeTypeLoop

                    mimeTypes += mimeType
                    if (mimeType.startsWith("audio/")) {
                        val bitrate = runCatching {
                            codecCapabilities.audioCapabilities?.bitrateRange?.upper?.toLong()
                        }.getOrNull()
                        if (bitrate != null) {
                            maxAudioBitrate = maxOf(maxAudioBitrate ?: 0L, bitrate)
                        }
                    }
                    if (mimeType.startsWith("video/")) {
                        val videoCapabilities = codecCapabilities.videoCapabilities
                        val bitrate = runCatching {
                            videoCapabilities?.bitrateRange?.upper?.toLong()
                        }.getOrNull()
                        val width = runCatching {
                            videoCapabilities?.supportedWidths?.upper
                        }.getOrNull()
                        val height = runCatching {
                            videoCapabilities?.supportedHeights?.upper
                        }.getOrNull()

                        if (bitrate != null) {
                            maxVideoBitrate = maxOf(maxVideoBitrate ?: 0L, bitrate)
                        }
                        if (width != null) {
                            maxWidth = maxOf(maxWidth ?: 0, width)
                        }
                        if (height != null) {
                            maxHeight = maxOf(maxHeight ?: 0, height)
                        }
                    }
                }
            }

        return MediaPlaybackCapabilities(
            supportedMimeTypes = mimeTypes,
            supportsAudio = mimeTypes.any { it.startsWith("audio/") },
            supportsVideo = mimeTypes.any { it.startsWith("video/") },
            maxAudioBitrate = maxAudioBitrate?.takeIf { it > 0 },
            maxVideoBitrate = maxVideoBitrate?.takeIf { it > 0 },
            maxWidth = maxWidth?.takeIf { it > 0 },
            maxHeight = maxHeight?.takeIf { it > 0 }
        )
    }
}
