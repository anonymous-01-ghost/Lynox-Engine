# Platform support

| Target | Intended artifact | Configuration | Verification |
|---|---|---|---|
| Android | AAR | Android library release variant | Pending CI |
| iOS device | XCFramework slice (iosArm64) | Static LynoxEngine.xcframework | Pending macOS CI |
| iOS simulator (Apple Silicon) | XCFramework slice (iosSimulatorArm64) | Static LynoxEngine.xcframework | Pending macOS CI |
| iOS simulator (Intel) | XCFramework slice (iosX64) | Static LynoxEngine.xcframework | Pending macOS CI |
| JVM desktop | JAR | JVM target | Pending CI |

These targets describe the library build configuration, not production validation of every source adapter. Each adapter must publish its own platform compatibility matrix.

## Build the iOS XCFramework

On macOS with Xcode installed:

    gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework

Expected output: lynox-engine/build/XCFrameworks/release/LynoxEngine.xcframework.

GitHub Actions builds this bundle and uploads it as a workflow artifact. A queued or running workflow is not a verified build; check the run result and confirm the artifact exists.

Do not advertise any target or adapter as production-ready until its build and relevant tests pass in CI on the appropriate runner.
