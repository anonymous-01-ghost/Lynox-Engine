package dev.lynox.engine.cache

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Small in-memory LRU cache with a time-to-live.
 *
 * This cache is intentionally process-local. It never persists provider data to disk.
 */
internal class LruTtlCache<K : Any, V : Any>(
    private val maxEntries: Int,
    private val ttl: Duration,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    private data class Entry<V>(
        val value: V,
        val createdAt: TimeMark
    )

    private val entries = linkedMapOf<K, Entry<V>>()

    init {
        require(maxEntries > 0) { "maxEntries must be greater than zero" }
        require(ttl.isPositive()) { "ttl must be positive" }
    }

    @Synchronized
    fun get(key: K): V? {
        val entry = entries.remove(key) ?: return null
        if (entry.createdAt.elapsedNow() >= ttl) return null
        entries[key] = entry
        return entry.value
    }

    @Synchronized
    fun put(key: K, value: V) {
        entries.remove(key)
        entries[key] = Entry(value, timeSource.markNow())
        while (entries.size > maxEntries) {
            val eldestKey = entries.keys.firstOrNull() ?: break
            entries.remove(eldestKey)
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
