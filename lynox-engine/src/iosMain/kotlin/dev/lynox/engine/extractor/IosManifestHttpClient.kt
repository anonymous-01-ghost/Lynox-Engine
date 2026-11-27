package dev.lynox.engine.extractor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.NSHTTPURLResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS transport backed by URLSession.
 *
 * URLSession applies the request timeout and follows standard HTTP redirects.
 * The response is checked against the configured limit before decoding. Note
 * that URLSession's data-task completion API buffers the response in NSData;
 * use a delegate-based streaming transport for strict pre-buffer memory bounds.
 */
@OptIn(ExperimentalForeignApi::class)
class IosManifestHttpClient : ManifestHttpClient {
    override suspend fun get(request: ManifestHttpRequest): ManifestHttpResponse {
        require(request.maxResponseChars > 0) { "maxResponseChars must be positive" }
        require(request.timeoutMillis > 0) { "timeoutMillis must be positive" }

        val url = NSURL.URLWithString(request.url)
            ?: throw IllegalArgumentException("Invalid manifest URL")
        val nativeRequest = NSMutableURLRequest.requestWithURL(url).apply {
            setHTTPMethod("GET")
            timeoutInterval = request.timeoutMillis.toDouble() / 1000.0
            setValue(
                "application/vnd.apple.mpegurl, audio/mpegurl, text/plain",
                forHTTPHeaderField = "Accept"
            )
        }

        return suspendCancellableCoroutine { continuation ->
            lateinit var task: NSURLSessionDataTask
            task = NSURLSession.sharedSession.dataTaskWithRequest(
                request = nativeRequest,
                completionHandler = { data: NSData?, response: platform.Foundation.NSURLResponse?, error: platform.Foundation.NSError? ->
                if (continuation.isCancelled) return@dataTaskWithRequest
                if (error != null) {
                    continuation.resumeWithException(
                        IllegalStateException(error.localizedDescription)
                    )
                    return@dataTaskWithRequest
                }

                val httpResponse = response as? NSHTTPURLResponse
                if (httpResponse == null) {
                    continuation.resumeWithException(
                        IllegalStateException("Manifest server returned a non-HTTP response")
                    )
                    return@dataTaskWithRequest
                }

                val payload = data ?: NSData()
                val maxBytes = request.maxResponseChars.toLong() * 4L
                if (payload.length > maxBytes.toULong()) {
                    continuation.resumeWithException(
                        IllegalStateException("Manifest response exceeds configured byte limit")
                    )
                    return@dataTaskWithRequest
                }

                val body = NSString.create(data = payload, encoding = NSUTF8StringEncoding) as? String
                if (body == null) {
                    continuation.resumeWithException(
                        IllegalStateException("Manifest response is not valid UTF-8")
                    )
                    return@dataTaskWithRequest
                }

                continuation.resume(
                    ManifestHttpResponse(
                        statusCode = httpResponse.statusCode.toInt(),
                        body = body,
                        contentType = httpResponse.MIMEType,
                        finalUrl = response.URL?.absoluteString
                    )
                )
                }
            )
            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        }
    }
}
