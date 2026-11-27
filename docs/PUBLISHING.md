# Release and local publication

The project coordinates are:

- Group: `dev.lynox`
- Module: `lynox-engine`
- Version: `0.1.0-SNAPSHOT` by default.

Override the version with `-PVERSION_NAME=0.1.0` for a release build.

## Publish to a local repository directory

After installing the required JDK, Gradle and Android SDK, run:

```bash
gradle :lynox-engine:publishAllPublicationsToBuildDirectoryRepository -PVERSION_NAME=0.1.0
```

The generated Maven repository is written under `lynox-engine/build/repository/`. Inspect generated publications before distributing them. Android AAR and JVM artifacts are produced by the corresponding Kotlin Multiplatform targets.

## Build the iOS XCFramework

On macOS with Xcode installed:

```bash
gradle :lynox-engine:assembleLynoxEngineReleaseXCFramework
```

The expected bundle path is `lynox-engine/build/XCFrameworks/release/LynoxEngine.xcframework`. It combines the configured iOS device and simulator architectures. GitHub Actions uploads this bundle as a workflow artifact after a successful build; it is not yet published to a remote package repository.

## Remote publication

No remote Maven repository, signing key, credential or automatic release is configured. Those require an explicit release destination and credential setup; secrets must never be committed to Git.
