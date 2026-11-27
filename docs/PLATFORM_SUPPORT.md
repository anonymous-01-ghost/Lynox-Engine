# Platform support

| Target | Intended artifact | Status |
|---|---|---|
| Android | AAR | Scaffold configured; build not yet verified |
| iOS device | XCFramework / framework | Target configured; requires macOS + Xcode validation |
| iOS simulator | XCFramework / framework | Target configured; requires macOS + Xcode validation |
| JVM desktop | JAR | Target configured; build not yet verified |

Do not advertise a target as production-ready until its build and tests pass in CI on the appropriate runner.
