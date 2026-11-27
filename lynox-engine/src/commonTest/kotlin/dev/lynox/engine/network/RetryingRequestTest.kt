package dev.lynox.engine.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryingRequestTest {
    @Test
    fun retriesServerErrorsAndReturnsSuccessfulResponse() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) {
                respond(
                    content = "temporary",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = Headers.build { append(HttpHeaders.RetryAfter, "0") }
                )
            } else {
                respond("ok", status = HttpStatusCode.OK)
            }
        })

        try {
            val response = executeWithRetry(
                baseDelayMillis = 0,
                maxDelayMillis = 0
            ) { client.get("https://example.test") }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ok", response.bodyAsText())
            assertEquals(2, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun retriesRateLimitedResponses() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls < 3) {
                respond("rate limited", status = HttpStatusCode.TooManyRequests)
            } else {
                respond("ok", status = HttpStatusCode.OK)
            }
        })

        try {
            val response = executeWithRetry(
                baseDelayMillis = 0,
                maxDelayMillis = 0
            ) { client.get("https://example.test") }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(3, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun stopsAfterConfiguredMaximumAttempts() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond("unavailable", status = HttpStatusCode.ServiceUnavailable)
        })

        try {
            val response = executeWithRetry(
                maxAttempts = 3,
                baseDelayMillis = 0,
                maxDelayMillis = 0
            ) { client.get("https://example.test") }
            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(3, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun doesNotRetryNonTransientClientErrors() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond("forbidden", status = HttpStatusCode.Forbidden)
        })

        try {
            val response = executeWithRetry(
                baseDelayMillis = 0,
                maxDelayMillis = 0
            ) { client.get("https://example.test") }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(1, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun doesNotRetryTransportExceptions() = runBlocking {
        var calls = 0
        val failure = assertFailsWith<IllegalStateException> {
            executeWithRetry(baseDelayMillis = 0, maxDelayMillis = 0) {
                calls++
                throw IllegalStateException("transport failed")
            }
        }
        assertEquals("transport failed", failure.message)
        assertEquals(1, calls)
    }

    @Test
    fun propagatesCancellationWithoutRetrying() = runBlocking {
        var calls = 0
        assertFailsWith<CancellationException> {
            executeWithRetry(baseDelayMillis = 0, maxDelayMillis = 0) {
                calls++
                throw CancellationException("cancel request")
            }
        }
        assertEquals(1, calls)
    }

}
