# Platform integration boundary

Lynox Engine is linked directly into the consuming application. There is no mandatory Lynox server, Google Cloud account, API key, or AI token.

The app supplies a source-specific MediaResolver implementation and passes the resolved, authorized rendition to its platform playback component.

## Android

Consume the dev.lynox:lynox-engine Android variant from Gradle. Implement the selected source adapter in Kotlin and use Android's playback component for the resolved rendition.

## iOS

Link LynoxEngine.xcframework in Xcode. Expose a Swift-friendly wrapper around the Kotlin API and use AVFoundation only for renditions supported by the source and application.

## Desktop JVM

Consume the JVM variant from Gradle/Maven. Implement the source adapter and connect the resolved rendition to the desktop application's playback layer.

## Status

These are integration boundaries, not complete runnable playback samples. No source adapter is declared production-ready until parser tests, error handling, refresh behavior and device playback are validated.
