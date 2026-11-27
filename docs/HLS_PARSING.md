# HLS master playlist parsing

Lynox Engine includes a platform-neutral `HlsMasterPlaylistParser` in `commonMain`.

## Contract

```kotlin
val parsed = HlsMasterPlaylistParser().parse(manifestUrl, manifestText)\n\n// Or use an application-provided transport:\nval resolver = HlsManifestResolver(httpClient = platformHttpClient)\nval resolved = resolver.resolve(manifestUrl)
```

The parser is deliberately transport-neutral: the application may obtain the manifest itself, or inject a platform-native `ManifestHttpClient` into `HlsManifestResolver`. The engine does not bundle a networking stack or require a server. The injected client must enforce the supplied response-size limit while reading, apply the timeout, follow only acceptable redirects, and propagate coroutine cancellation.

## Supported master-playlist fields

- `#EXT-X-STREAM-INF`: bandwidth (prefers average bandwidth), resolution, frame rate, codecs and variant URI.
- `#EXT-X-MEDIA:TYPE=AUDIO`: external audio rendition URI associated by group ID.
- Absolute, scheme-relative, root-relative and path-relative HTTP(S) rendition URLs.

The result is a `ResolvedMedia` with HLS rendition URLs, MIME type `application/vnd.apple.mpegurl`, and available metadata. A variant with a separate audio group is represented by a VIDEO rendition plus an AUDIO rendition.

## Deliberate limits

- This is a master-playlist parser, not a media-segment downloader or HLS playback engine.
- It does not decrypt segments, interpret DRM, obtain credentials, bypass access restrictions, or refresh signed URLs.
- It does not yet parse media-playlist segment timelines, encryption keys, subtitles, I-frame variants, or rendition selection rules such as language and DEFAULT/AUTOSELECT.
- `HlsManifestResolver` validates HTTP status, optional playlist content type, final redirect URL and response character limit. It maps transport failures to typed engine errors and propagates coroutine cancellation.\n- The transport is injected: implementations are still required for Android, iOS and JVM. Timeout enforcement, redirect policy and byte-level streaming limits belong to that transport.

## Validation

Common parser tests cover relative URL resolution, bitrate and resolution metadata, quoted codec attributes containing commas, separate audio renditions, invalid headers, manifests without variants and non-HTTP(S) manifest URLs. Resolver tests cover successful loading, HTTP authorization/server errors, oversized bodies, invalid content types and transport exceptions.


## Native transport implementations

- Android: `AndroidManifestHttpClient` uses `HttpURLConnection` on `Dispatchers.IO`, applies connect/read timeouts, follows HTTP redirects and bounds bytes read.
- JVM: `JvmManifestHttpClient` provides the same behavior for desktop/server-side JVM consumers without requiring a service.
- iOS: `IosManifestHttpClient` uses `URLSession`, applies request timeout, follows standard redirects and supports coroutine cancellation. Its current data-task implementation buffers NSData before checking the configured byte ceiling; a delegate-based streaming implementation is still required for strict pre-buffer memory bounds.
- The transport implementations are platform-specific and are not automatically selected. Inject the implementation for the target platform into `HlsManifestResolver`.
- The consuming app remains responsible for platform network policy, including Android cleartext restrictions and iOS App Transport Security.
