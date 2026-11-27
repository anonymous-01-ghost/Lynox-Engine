package dev.lynox.engine.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class LynoxConfig(
    val cacheTtl: Duration = 15.minutes,
    val maxCacheEntries: Int = 200
) {
    init {
        require(cacheTtl.isPositive()) { "cacheTtl must be positive" }
        require(maxCacheEntries > 0) { "maxCacheEntries must be greater than zero" }
    }
}
