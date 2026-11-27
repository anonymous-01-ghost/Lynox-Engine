# HLS master playlist parsing

Lynox Engine includes a platform-neutral `HlsMasterPlaylistParser` in `commonMain`.

## Contract

```kotlin
val result = HlsMasterPlaylistParser().parse(manifestUrl, manifestText)
```

The parser is deliberately transport-neutral: the application obtains the manifest using an access method permitted by its source and passes the URL and text to the parser. The parser makes no network requests and has no server dependency.

## Supported master-playlist fields

- `#EXT-X-STREAM-INF`: bandwidth (prefers average bandwidth), resolution, frame rate, codecs and variant URI.
- `#EXT-X-MEDIA:TYPE=AUDIO`: external audio rendition URI associated by group ID.
- Absolute, scheme-relative, root-relative and path-relative HTTP(S) rendition URLs.

The result is a `ResolvedMedia` with HLS rendition URLs, MIME type `application/vnd.apple.mpegurl`, and available metadata. A variant with a separate audio group is represented by a VIDEO rendition plus an AUDIO rendition.

## Deliberate limits

- This is a master-playlist parser, not a media-segment downloader or HLS playback engine.
- It does not decrypt segments, interpret DRM, obtain credentials, bypass access restrictions, or refresh signed URLs.
- It does not yet parse media-playlist segment timelines, encryption keys, subtitles, I-frame variants, or rendition selection rules such as language and DEFAULT/AUTOSELECT.
- The caller must pass a valid, accessible manifest and handle network errors, redirects, expiry and refresh.

## Validation

Common tests cover relative URL resolution, bitrate and resolution metadata, quoted codec attributes containing commas, separate audio renditions, invalid headers, manifests without variants and non-HTTP(S) manifest URLs.
