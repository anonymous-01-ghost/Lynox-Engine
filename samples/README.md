# Samples

Samples will demonstrate direct in-process dependency consumption on Android, iOS and JVM.

The sample apps must use a real, permitted source adapter once one is implemented. Until then, they may validate dependency resolution and the source-neutral API contract, but must not claim end-to-end media extraction or playback.

Planned:
- Android: Kotlin app consuming the AAR.
- iOS: Swift app importing the XCFramework.
- Desktop: Kotlin/JVM app consuming the JAR.
