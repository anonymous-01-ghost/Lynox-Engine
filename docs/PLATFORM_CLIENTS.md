# Platform HTTP client setup

Lynox Engine is a client-side Kotlin Multiplatform library. It does not require a Lynox server. The consuming application creates one Ktor `HttpClient` with the engine for its platform and injects that client into the Lynox providers.

The library module already declares these Ktor engines:

| Target | Ktor engine |
| --- | --- |
| Android | `ktor-client-android` |
| iOS (device and simulator) | `ktor-client-darwin` |
| JVM desktop | `ktor-client-cio` |

If you create the client in the consuming application rather than inside the library, add the corresponding Ktor engine dependency to that application's platform source set as well. Keep the Ktor version aligned with the version used by Lynox Engine.

## Android

In the Android application module, add `ktor-client-android` and create the client in an application-scoped owner (not inside each search call):

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

val lynoxHttpClient = HttpClient(Android) {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
```

Close the client when the component that owns the Lynox engine is permanently disposed:

```kotlin
lynoxHttpClient.close()
```

## iOS

In the iOS application source set, add `ktor-client-darwin`:

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

val lynoxHttpClient = HttpClient(Darwin) {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
```

Retain the client for the lifetime of the app/service that uses Lynox Engine, then call `close()` when that owner is disposed.

## JVM desktop

In the desktop JVM source set, add `ktor-client-cio`:

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

val lynoxHttpClient = HttpClient(CIO) {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
```

Close the client during application shutdown.

## Injecting the client

Use the same application-scoped client for the video and catalog adapters:

```kotlin
val videoProvider = YouTubeDataApiProvider(
    httpClient = lynoxHttpClient,
    apiKey = restrictedYouTubeApiKey
)

val catalogProvider = YouTubeDataApiCatalogProvider(
    httpClient = lynoxHttpClient,
    apiKey = restrictedYouTubeApiKey
)

val engine = LynoxEngine(
    searchProvider = videoProvider,
    metadataProvider = videoProvider,
    catalogProvider = catalogProvider
)
```

Do not instantiate a new `HttpClient` for each request. Ktor clients own connection pools and other resources; reuse the client and close it exactly once when its owner is finished.

## Timeout and error behavior

- The sample values (15 seconds to connect, 30 seconds for request/socket) are starting points; tune them for the host application's UX and network conditions.
- Keep `expectSuccess = false` so Lynox can inspect HTTP status codes and apply its typed provider error handling and transient retry policy.
- Do not log complete request URLs: the YouTube API key is sent as a query parameter.
- A client-side API key is extractable from a distributed application. Restrict it in Google Cloud, monitor quota, and do not treat it as a secret.
- These examples configure networking only. YouTube Data API search and metadata do not provide raw media streams or authorize scraping or playback extraction.

