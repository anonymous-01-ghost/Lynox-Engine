# Platform integration examples

The core API is shared. Only the Ktor engine differs by platform.

## Android (Kotlin)

```kotlin
import dev.lynox.engine.LynoxEngine
import dev.lynox.engine.provider.YouTubeDataApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

val client = HttpClient(Android)
val provider = YouTubeDataApiProvider(
    httpClient = client,
    apiKey = BuildConfig.YOUTUBE_DATA_API_KEY
)
val lynox = LynoxEngine(
    searchProvider = provider,
    metadataProvider = provider
)

// Call lynox.search(...) from a coroutine.
// Close client when the owning application component is disposed.
```

Do not hardcode a production key in source. Inject it from build configuration, restrict it in Google Cloud, and assess the risk of distributing a client-side key.

## iOS (Kotlin Multiplatform)

In shared Kotlin code:

```kotlin
import dev.lynox.engine.LynoxEngine
import dev.lynox.engine.provider.YouTubeDataApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

val client = HttpClient(Darwin)
val provider = YouTubeDataApiProvider(
    httpClient = client,
    apiKey = apiKey
)
val lynox = LynoxEngine(
    searchProvider = provider,
    metadataProvider = provider
)
```

Export the static `LynoxEngine` framework from the iOS target and expose an application-facing wrapper if the Swift app needs a simpler API. Keep API-key provisioning outside the framework.

## Desktop JVM (Kotlin)

```kotlin
import dev.lynox.engine.LynoxEngine
import dev.lynox.engine.provider.YouTubeDataApiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

val client = HttpClient(CIO)
val provider = YouTubeDataApiProvider(client, apiKey)
val lynox = LynoxEngine(provider, provider)
```

The desktop application owns and closes the client on shutdown.

## Scope

These examples configure search and metadata only. They do not create a media stream URL or a custom YouTube playback implementation.
