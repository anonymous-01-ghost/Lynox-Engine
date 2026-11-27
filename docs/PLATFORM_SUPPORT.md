# Platform support

| Target | Intended artifact | Configuration | Verification |
|---|---|---|---|
| Android | AAR | Android library release variant | Pending CI |
| iOS device | XCFramework slice (`iosArm64`) | Included in static `LynoxEngine.xcframework` | Pending macOS CI |
| iOS simulator (Apple Silicon) | XCFramework slice (`iosSimulatorArm64`) | Included in static `LynoxEngine.xcframework` | Pending macOS CI |
| iOS simulator (Intel) | XCFramework slice (`iosX64`) | Included in static `LynoxEngine.xcframework` | Pending macOS CI |
| JVM desktop | JAR | JVM target | Pending CI |

## Build the iOS XCFramework

On macOS with Xcode installed:

```bash
gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework
```

Expected output:

```text
lynox-engine/build/XCFrameworks/release/LynoxEngine.xcframework
```

GitHub Actions builds this bundle and uploads it as a workflow artifact. A queued or running workflow is not a verified build; check the run result and confirm the artifact exists.

Do not advertise any target as production-ready until its build and relevant tests pass in CI on the appropriate runner.
