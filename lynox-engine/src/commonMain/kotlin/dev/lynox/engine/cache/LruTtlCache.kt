package dev.lynox.engine.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Small in-memory LRU cache with a time-to-live.
 *
 * This cache is process-local and safe for concurrent coroutine access.
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

    private val mutex = Mutex()
    private val entries = linkedMapOf<K, Entry<V>>()

    init {
        require(maxEntries > 0) { "maxEntries must be greater than zero" }
        require(ttl.isPositive()) { "ttl must be positive" }
    }

    suspend fun get(key: K): V? = mutex.withLock {
        val entry = entries.remove(key) ?: return@withLock null
        if (entry.createdAt.elapsedNow() >= ttl) return@withLock null
        entries[key] = entry
        entry.value
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        entries.remove(key)
        entries[key] = Entry(value, timeSource.markNow())
        while (entries.size > maxEntries) {
            val eldestKey = entries.keys.firstOrNull() ?: break
            entries.remove(eldestKey)
        }
    }

    suspend fun clear() = mutex.withLock {
        entries.clear()
    }
}
