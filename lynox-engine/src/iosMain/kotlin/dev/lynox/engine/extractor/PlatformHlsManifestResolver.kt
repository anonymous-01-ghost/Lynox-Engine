package dev.lynox.engine.extractor

/**
 * Creates an HLS manifest resolver using Apple's URLSession transport.
 *
 * Pass the returned resolver to [dev.lynox.engine.LynoxEngine] as mediaResolver.
 */
fun createPlatformHlsManifestResolver(): MediaResolver =
    HlsManifestResolver(IosManifestHttpClient())
