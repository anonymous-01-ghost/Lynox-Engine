# Platform HTTP client setup

Lynox Engine is a client-side Kotlin Multiplatform library. The core does not require a Lynox server, Google Cloud project, YouTube Data API key, or AI service. A source adapter may use Ktor when its source protocol requires HTTP; the consuming application owns the client lifecycle.

## Platform engines

| Target | Ktor engine |
| --- | --- |
| Android | ktor-client-android |
| iOS | ktor-client-darwin |
| JVM desktop | ktor-client-cio |

Create one application-scoped client rather than a new client for every resolution request. Configure timeouts appropriate to the source and close the client when its owner is disposed.

## Android

    val client = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

## iOS

    val client = HttpClient(Darwin) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

## JVM desktop

    val client = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }

Add the matching Ktor engine dependency to the consuming app if it creates the client. Keep versions aligned with the library and close the client exactly once.

## Source adapter responsibilities

A MediaResolver implementation may receive an injected HttpClient, but the generic contract does not mandate Ktor or a particular network strategy. The adapter is responsible for source-specific redirects, headers, parsing, retries, expiration and refresh behavior. Never log authorization headers, cookies, tokens or signed URLs.

## Optional legacy metadata providers

The repository retains optional YouTube Data API adapters for search and metadata only. They require an API key if explicitly used, but they are not part of the extractor core and do not resolve media streams.
