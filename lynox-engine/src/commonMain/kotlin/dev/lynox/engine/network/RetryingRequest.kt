package dev.lynox.engine.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay

/**
 * Retries only transient HTTP responses. Caller cancellation and transport exceptions
 * are never swallowed or retried here.
 */
internal suspend fun executeWithRetry(
    maxAttempts: Int = 3,
    baseDelayMillis: Long = 250,
    maxDelayMillis: Long = 2_000,
    request: suspend () -> HttpResponse
): HttpResponse {
    require(maxAttempts >= 1) { "maxAttempts must be at least one" }
    require(baseDelayMillis >= 0) { "baseDelayMillis cannot be negative" }
    require(maxDelayMillis >= baseDelayMillis) { "maxDelayMillis must be >= baseDelayMillis" }

    var attempt = 1
    while (true) {
        val response = request()
        val status = response.status.value
        val retryable = status == 429 || status in 500..599
        if (!retryable || attempt >= maxAttempts) return response

        // Consume the transient response before opening the next request.
        response.bodyAsText()
        val exponentialDelay = (baseDelayMillis * (1L shl (attempt - 1).coerceAtMost(10)))
            .coerceAtMost(maxDelayMillis)
        val retryAfter = response.headers[HttpHeaders.RetryAfter]
            ?.toLongOrNull()
            ?.times(1_000L)
            ?.coerceAtMost(maxDelayMillis)
        delay(retryAfter ?: exponentialDelay)
        attempt++
    }
}
