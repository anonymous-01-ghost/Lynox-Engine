package dev.lynox.engine.extractor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSISOLatin1StringEncoding
import platform.Foundation.create
import platform.Foundation.setValue
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS transport backed by URLSession delegate callbacks.
 *
 * Response chunks are accumulated only up to the configured byte ceiling.
 * URLSession may allocate an individual incoming chunk before delivering it
 * to the delegate, but the client never appends data beyond its own limit.
 */
@OptIn(ExperimentalForeignApi::class)
class IosManifestHttpClient : ManifestHttpClient {
    override suspend fun get(request: ManifestHttpRequest): ManifestHttpResponse {
        require(request.maxResponseChars > 0) { "maxResponseChars must be positive" }
        require(request.timeoutMillis > 0) { "timeoutMillis must be positive" }

        val url = NSURL.URLWithString(request.url)
            ?: throw IllegalArgumentException("Invalid manifest URL")
        val nativeRequest = NSMutableURLRequest.requestWithURL(url).apply {
            setTimeoutInterval(request.timeoutMillis.toDouble() / 1000.0)
            setValue(
                "application/vnd.apple.mpegurl, audio/mpegurl, text/plain",
                forHTTPHeaderField = "Accept"
            )
        }
        val maxBytes = request.maxResponseChars.toLong() * 4L

        return suspendCancellableCoroutine { continuation ->
            lateinit var session: NSURLSession
            lateinit var task: NSURLSessionDataTask
            val delegate = ManifestResponseDelegate(continuation, maxBytes)
            session = NSURLSession.sessionWithConfiguration(
                NSURLSessionConfiguration.defaultSessionConfiguration(),
                delegate,
                delegateQueue = NSOperationQueue.mainQueue()
            )
            task = session.dataTaskWithRequest(nativeRequest)
            continuation.invokeOnCancellation {
                task.cancel()
                session.invalidateAndCancel()
            }
            task.resume()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ManifestResponseDelegate(
    private val continuation: CancellableContinuation<ManifestHttpResponse>,
    private val maxBytes: Long
) : NSObject(), NSURLSessionDataDelegateProtocol {
    private val receivedChunks = mutableListOf<String>()
    private var receivedByteCount = 0L
    private var responseTooLarge = false

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData
    ) {
        val nextSize = receivedByteCount + didReceiveData.length.toLong()
        if (nextSize > maxBytes) {
            responseTooLarge = true
            dataTask.cancel()
            return
        }
        // Latin-1 maps each input byte to one Unicode code point, allowing
        // chunks to be joined without corrupting UTF-8 sequences split at boundaries.
        val chunk = NSString.create(
            data = didReceiveData,
            encoding = NSISOLatin1StringEncoding
        ) as? String
        if (chunk == null) {
            responseTooLarge = true
            dataTask.cancel()
            return
        }
        receivedChunks += chunk
        receivedByteCount = nextSize
    }

    override fun URLSession(
        session: NSURLSession,
        task: platform.Foundation.NSURLSessionTask,
        didCompleteWithError: platform.Foundation.NSError?
    ) {
        if (!continuation.isActive) return

        if (responseTooLarge) {
            continuation.resumeWithException(
                IllegalStateException("Manifest response exceeds configured byte limit")
            )
            session.invalidateAndCancel()
            return
        }

        if (didCompleteWithError != null) {
            continuation.resumeWithException(
                IllegalStateException(didCompleteWithError.localizedDescription)
            )
            session.finishTasksAndInvalidate()
            return
        }

        val httpResponse = task.response as? NSHTTPURLResponse
        if (httpResponse == null) {
            continuation.resumeWithException(
                IllegalStateException("Manifest server returned a non-HTTP response")
            )
            session.finishTasksAndInvalidate()
            return
        }

        val latin1 = buildString {
            receivedChunks.forEach(::append)
        }
        val bytes = ByteArray(latin1.length) { index -> latin1[index].code.toByte() }
        val body = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }.getOrNull()
        if (body == null) {
            continuation.resumeWithException(
                IllegalStateException("Manifest response is not valid UTF-8")
            )
            session.finishTasksAndInvalidate()
            return
        }

        continuation.resume(
            ManifestHttpResponse(
                statusCode = httpResponse.statusCode.toInt(),
                body = body,
                contentType = httpResponse.MIMEType,
                finalUrl = httpResponse.URL?.absoluteString
            )
        )
        session.finishTasksAndInvalidate()
    }
}
