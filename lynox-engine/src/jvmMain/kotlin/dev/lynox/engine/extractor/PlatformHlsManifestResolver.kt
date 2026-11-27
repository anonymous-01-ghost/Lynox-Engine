package dev.lynox.engine.extractor

/**
 * Creates an HLS manifest resolver using the JVM's native HTTP transport.
 *
 * Pass the returned resolver to [dev.lynox.engine.LynoxEngine] as mediaResolver.
 */
fun createPlatformHlsManifestResolver(): MediaResolver =
    HlsManifestResolver(JvmManifestHttpClient())
