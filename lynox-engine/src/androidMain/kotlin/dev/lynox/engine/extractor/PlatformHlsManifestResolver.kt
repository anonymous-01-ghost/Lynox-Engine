package dev.lynox.engine.extractor

/**
 * Creates an HLS manifest resolver using Android's native HTTP transport.
 *
 * Pass the returned resolver to [dev.lynox.engine.LynoxEngine] as mediaResolver.
 */
fun createPlatformHlsManifestResolver(): MediaResolver =
    HlsManifestResolver(AndroidManifestHttpClient())
