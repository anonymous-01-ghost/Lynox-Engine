package dev.lynox.engine.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

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
            val response = executeWithRetry { client.get("https://example.test") }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(2, calls)
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
            val response = executeWithRetry { client.get("https://example.test") }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(1, calls)
        } finally {
            client.close()
        }
    }
}
