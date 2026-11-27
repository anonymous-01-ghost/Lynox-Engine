# Caching

Lynox Engine caches successful search pages and metadata responses in process memory. The cache is LRU-bounded and each entry expires after the configured TTL.

    val engine = LynoxEngine(
        searchProvider = mySearchProvider,
        metadataProvider = myMetadataProvider,
        config = LynoxConfig(
            cacheTtl = 10.minutes,
            maxCacheEntries = 100
        )
    )

    engine.clearCache()

Failures are never cached. Search cache keys include the normalized query, page token and page size. Metadata keys use the trimmed media ID.

Resolved media stream URLs are deliberately not cached by resolveMedia(). Signed URLs can expire independently of ordinary metadata; each source adapter is responsible for refreshing them according to its source's rules.

This cache is process-local and memory-only. It does not persist provider responses to disk.
