# Caching

Lynox Engine caches successful search pages and metadata responses in process memory.
The cache is LRU-bounded and each entry expires after the configured TTL.

```kotlin
val engine = LynoxEngine(
    searchProvider = youtubeProvider,
    metadataProvider = youtubeProvider,
    config = LynoxConfig(
        cacheTtl = 10.minutes,
        maxCacheEntries = 100
    )
)

// Explicitly invalidate both caches after a refresh or account/context change.
engine.clearCache()
```

Failures are never cached. Search cache keys include the normalized query, page token,
and page size so different pages cannot collide. Metadata keys use the trimmed media ID.

This cache is process-local and memory-only. It does not persist YouTube responses to disk.
