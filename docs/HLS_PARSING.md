# HLS master playlist parsing

Lynox Engine includes a platform-neutral `HlsMasterPlaylistParser` in `commonMain`.

## Contract

```kotlin
val parsed = HlsMasterPlaylistParser().parse(manifestUrl, manifestText)\n\n// Or use an application-provided transport:\nval resolver = HlsManifestResolver(httpClient = platformHttpClient)\nval resolved = resolver.resolve(manifestUrl)
```

The parser is deliberately transport-neutral: the application may obtain the manifest itself, or inject a platform-native `ManifestHttpClient` into `HlsManifestResolver`. The engine does not bundle a networking stack or require a server. The injected client must enforce the supplied response-size limit while reading, apply the timeout, follow only acceptable redirects, and propagate coroutine cancellation.

## Supported master-playlist fields

- `#EXT-X-STREAM-INF`: bandwidth (prefers average bandwidth), resolution, frame rate, codecs and variant URI.
- `#EXT-X-MEDIA:TYPE=AUDIO`: all external audio rendition URIs associated by group ID, including `LANGUAGE`, `NAME`, `DEFAULT`, and `AUTOSELECT` metadata.
- Absolute, scheme-relative, root-relative and path-relative HTTP(S) rendition URLs, including query-only references and fragments on relative rendition paths.

The result is a `ResolvedMedia` with HLS rendition URLs, MIME type `application/vnd.apple.mpegurl`, and available metadata. A variant with a separate audio group is represented by a VIDEO rendition plus every declared AUDIO rendition in that group. Audio renditions expose their language, display label, default flag, and autoselect flag. Audio groups are resolved independently of whether their tags occur before or after the associated variant. `MediaStreamSelector` can prefer an exact language tag or a matching primary language subtag, and can optionally prefer `AUTOSELECT=YES` renditions.

## Deliberate limits

- This is a master-playlist parser, not a media-segment downloader or HLS playback engine.
- It does not decrypt segments, interpret DRM, obtain credentials, bypass access restrictions, or refresh signed URLs.
- It does not yet parse media-playlist segment timelines, encryption keys, subtitles, I-frame variants, language metadata is parsed and exposed, but automatic subtitle selection and playlist-level language policy remain the caller's responsibility.
- `HlsManifestResolver` validates HTTP status, optional playlist content type, final redirect URL and response character limit. It maps transport failures to typed engine errors and propagates coroutine cancellation.\n- The transport is injected. Android, iOS and JVM implementations are included; platform-specific factories are available from each target source set. Timeout enforcement, redirect policy and byte-level streaming limits belong to the transport.

## Validation

Common parser tests cover relative URL resolution, query-only references and relative-path fragments, bitrate and resolution metadata, quoted codec attributes containing commas, multiple audio renditions, default audio flags, audio groups declared after variants, invalid headers, manifests without variants and non-HTTP(S) manifest URLs. Resolver tests cover successful loading, HTTP authorization/server errors, oversized bodies, invalid content types and transport exceptions.


## Native transport implementations

- Android: `AndroidManifestHttpClient` uses `HttpURLConnection` on `Dispatchers.IO`, applies connect/read timeouts, follows HTTP redirects and bounds bytes read.
- JVM: `JvmManifestHttpClient` provides the same behavior for desktop/server-side JVM consumers without requiring a service.
- iOS: `IosManifestHttpClient` uses `URLSession` delegate callbacks, applies request timeout, follows standard redirects, supports coroutine cancellation and accumulates received chunks only up to the configured byte ceiling. URLSession may allocate an individual incoming chunk before the delegate receives it.
- `createPlatformHlsManifestResolver()` creates an `HlsManifestResolver` with the native transport for the current target. Pass it to `LynoxEngine(mediaResolver = ...)`. The factory is platform-specific and should be called from the corresponding Android, iOS or JVM source set.
- The consuming app remains responsible for platform network policy, including Android cleartext restrictions and iOS App Transport Security.


## Subtitle renditions

The master-playlist parser also exposes subtitle renditions declared with
`#EXT-X-MEDIA:TYPE=SUBTITLES` when a stream variant references their group
through its `SUBTITLES` attribute. Each `MediaSubtitleTrack` includes its
resolved playlist URL, optional language and display label, and the
`DEFAULT`, `AUTOSELECT`, and `FORCED` flags.

Subtitle playlists are metadata only at this stage: Lynox Engine does not
download subtitle segments, parse subtitle payloads, or perform DRM handling.
The playback client remains responsible for choosing and rendering a supported
subtitle format.
