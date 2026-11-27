# Consuming Lynox Engine

Lynox Engine is an in-process Kotlin Multiplatform library. A consuming application supplies its own Ktor `HttpClient` and YouTube Data API key. No Lynox-hosted API or server is required.

The project is currently pre-1.0. Treat the API and coordinates as subject to change.

## 1. Build and publish to a local Maven repository

From the Lynox Engine checkout, with JDK 17, Gradle 8.14.3 and the Android SDK installed:

```bash
gradle :lynox-engine:publishAllPublicationsToBuildDirectoryRepository -PVERSION_NAME=0.1.0-SNAPSHOT
```

The repository is generated at:

```text
lynox-engine/build/repository/
```

The configured Maven coordinates are:

- Group: `dev.lynox`
- Module: `lynox-engine`
- Version: `0.1.0-SNAPSHOT` (unless overridden)

The generated repository and its Gradle module metadata are authoritative for the exact target variants and artifact files. Inspect them before distributing or integrating a release. Do not infer target-specific artifact names from the project directory name.

## 2. Add the local repository to a Kotlin Gradle consumer

In the consuming project's `settings.gradle.kts`, add the generated repository to dependency resolution. Adjust the relative path to the actual Lynox Engine checkout:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("../Lynox-Engine/lynox-engine/build/repository")
        }
    }
}
```

Then add the library dependency to the module that needs it:

```kotlin
dependencies {
    implementation("dev.lynox:lynox-engine:0.1.0-SNAPSHOT")
}
```

Gradle Module Metadata is used for Kotlin Multiplatform variant selection. If Gradle cannot resolve the requested target, inspect the generated `.module` file and the publications in the local repository before changing coordinates.

## 3. Add the platform Ktor engine

The application creates and owns the HTTP client. Add the matching Ktor engine dependency to the application module, keeping its version aligned with Lynox Engine's Ktor version (`3.2.3` in this source revision):

Android:

```kotlin
dependencies {
    implementation("io.ktor:ktor-client-android:3.2.3")
}
```

JVM desktop:

```kotlin
dependencies {
    implementation("io.ktor:ktor-client-cio:3.2.3")
}
```

Kotlin Multiplatform iOS source set:

```kotlin
sourceSets {
    val iosMain by getting {
        dependencies {
            implementation("io.ktor:ktor-client-darwin:3.2.3")
        }
    }
}
```

For client construction, timeout configuration, lifecycle and API-key handling, follow [PLATFORM_CLIENTS.md](PLATFORM_CLIENTS.md).

## 4. Create the engine and perform a search

This Kotlin code is shared across Android and JVM, and can be called from Kotlin code in an iOS target:

```kotlin
import dev.lynox.engine.LynoxEngine
import dev.lynox.engine.model.EngineResult
import dev.lynox.engine.model.SearchRequest
import dev.lynox.engine.provider.YouTubeDataApiCatalogProvider
import dev.lynox.engine.provider.YouTubeDataApiProvider
import io.ktor.client.HttpClient

fun createLynoxEngine(
    httpClient: HttpClient,
    restrictedYouTubeApiKey: String
): LynoxEngine {
    val videos = YouTubeDataApiProvider(
        httpClient = httpClient,
        apiKey = restrictedYouTubeApiKey
    )
    val catalog = YouTubeDataApiCatalogProvider(
        httpClient = httpClient,
        apiKey = restrictedYouTubeApiKey
    )

    return LynoxEngine(
        searchProvider = videos,
        metadataProvider = videos,
        catalogProvider = catalog
    )
}

suspend fun searchVideos(engine: LynoxEngine, query: String) {
    when (val result = engine.search(SearchRequest(query = query))) {
        is EngineResult.Success -> {
            result.value.items.forEach { video ->
                println("${video.title}: ${video.webUrl}")
            }
        }
        is EngineResult.Failure -> {
            // Handle the typed EngineError; do not parse provider message text.
            println(result.error)
        }
    }
}
```

The same `HttpClient` can be shared by the video and catalog providers. Close it once when its application-scoped owner is disposed.

## 5. iOS integration boundary

The Gradle module currently declares static Kotlin/Native frameworks for iOS device (`iosArm64`) and simulators (`iosSimulatorArm64`, `iosX64`). These are architecture-specific framework targets.

A Swift/Xcode application needs a framework or XCFramework integrated into its Xcode project. Do not assume that the Maven dependency alone creates a ready-to-embed universal XCFramework. Build and package the required iOS framework for the intended architectures on macOS with Xcode, then integrate the resulting framework using the project's Xcode build workflow.

The current CI compiles each iOS framework target separately. A packaged XCFramework and a Swift-facing sample app are not yet declared as release deliverables.

## 6. Credentials, API limits and playback

- Never commit API keys or place them in source control.
- A key embedded in a distributed client can be extracted. Restrict it in Google Cloud to the intended API and application where supported, monitor quota, and do not treat it as a secret.
- The official YouTube Data API provides search and public metadata; it does not provide raw media streams.
- Lynox Engine does not scrape private endpoints, bypass ads/DRM, or implement unauthorized playback.

## 7. Verification status

A successful local publication task and platform CI jobs are required before calling a release verified. Check [GitHub Actions](https://github.com/anonymous-01-ghost/Lynox-Engine/actions) for the status of the current revision. A queued workflow is not a passing build.
