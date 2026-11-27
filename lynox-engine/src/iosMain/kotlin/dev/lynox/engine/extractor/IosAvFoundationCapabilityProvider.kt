package dev.lynox.engine.extractor

import platform.AVFoundation.AVURLAsset

/**
 * Reports the audiovisual MIME types recognized by AVFoundation on this Apple
 * platform. This is a framework-level format list, not a guarantee for every
 * codec profile, stream configuration, or active AVPlayer item.
 */
class IosAvFoundationCapabilityProvider : MediaPlaybackCapabilityProvider {

    override suspend fun getCapabilities(): MediaPlaybackCapabilities {
        val mimeTypes = AVURLAsset.audiovisualMIMETypes()
            .mapNotNull { it as? String }
            .map { it.lowercase() }
            .toSet()

        return MediaPlaybackCapabilities(
            supportedMimeTypes = mimeTypes,
            supportsAudio = mimeTypes.any { it.startsWith("audio/") },
            supportsVideo = mimeTypes.any { it.startsWith("video/") }
        )
    }
}
