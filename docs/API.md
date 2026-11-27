# Public API

This document describes the pre-1.0 public API. It is not yet a stable 1.0 contract.

## Media extraction

LynoxEngine accepts a source-specific MediaResolver:

    val engine = LynoxEngine(mediaResolver = mySourceResolver)
    val result = engine.resolveMedia("https://media.example/source/item")

The resolver contract:

    interface MediaResolver {
        suspend fun resolve(sourceUrl: String): EngineResult<ResolvedMedia>
    }

The core validates that the source URL uses HTTP or HTTPS, normalizes surrounding whitespace, and delegates resolution. Resolved stream URLs are deliberately not cached because they may be signed or short-lived.

ResolvedMedia contains the source URL, optional title/duration, a non-empty list of MediaStream renditions, and optional expiry time. Each MediaStream can describe audio, video, or combined media with MIME type, codecs, bitrate, dimensions, frame rate and content length.

### Built-in direct-file adapter

DirectUrlMediaResolver handles direct HTTP(S) media-file URLs with recognized extensions (MP3, M4A, AAC, OGG, OPUS, WAV, FLAC, MP4, WEBM, MOV, MKV and related variants). It does not make a network request; it classifies the supplied file URL and returns it as a single rendition.

Example:

    val engine = LynoxEngine(mediaResolver = DirectUrlMediaResolver())
    val result = engine.resolveMedia("https://cdn.example.test/audio/song.m4a")

This adapter does not search websites, parse watch pages, enumerate playlists, or extract media from third-party services. A recognized extension does not guarantee that the remote server will serve a playable file; the playback client must handle HTTP and codec errors.

## Rendition selection

MediaStreamSelector selects one rendition from metadata already returned by a resolver. MediaSelectionRequest controls the requested media kind (audio, video, or either), ranking strategy (best quality, lowest bandwidth, or prefer default), and optional bitrate/dimension limits. When a limit is set, a rendition with a missing value for that limit is excluded because compliance cannot be verified. The selector does not probe or download remote URLs.

### Source-specific adapters

The generic contract does not include a built-in adapter for YouTube or any other watch-page service. A source-specific implementation must use access methods permitted for that source and be tested before media extraction is available for it.

## Search and catalog contracts

SearchProvider, MetadataProvider and CatalogProvider remain provider-neutral extension points. The repository no longer includes an official YouTube Data API adapter. These contracts do not imply that an arbitrary source supports search, channels, playlists, or metadata.

## Results and errors

Operations use EngineResult.Success or EngineResult.Failure. Expected operational errors are represented by EngineError.

## Stability

Names and signatures may change before version 1.0. Every new source adapter must include parser fixtures, failure cases, and platform integration tests before being called production-ready.


### Playback-client capabilities

`MediaPlaybackCapabilities` lets an application describe the formats and limits accepted by its actual playback client. It can declare supported MIME types (including major-type wildcards such as `video/*`), codec identifiers, audio/video support, bitrate ceilings and maximum dimensions. Pass it through `MediaSelectionRequest.capabilities`; the selector removes renditions that cannot be verified as compatible before ranking them.

An empty MIME or codec set means that capability is unrestricted by the declaration. If a non-empty codec set is supplied, a rendition with missing codec metadata is excluded. Platform-specific modules should populate this model from the real player/device capability APIs. The common module does not claim that Android, iOS or JVM supports any particular codec by default, and it does not itself decode or play media.


### Capability providers

Applications can supply a `MediaPlaybackCapabilityProvider` that reports the active playback client's current capabilities. `StaticMediaPlaybackCapabilityProvider` is included for fixed profiles and tests. Platform-specific providers must query the actual playback stack and return a `MediaPlaybackCapabilities` value; the core intentionally does not guess codec support from the operating-system name.

Example:

    val provider = StaticMediaPlaybackCapabilityProvider(
        MediaPlaybackCapabilities(
            supportedMimeTypes = setOf("audio/mp4"),
            supportedCodecs = setOf("mp4a.40.2"),
            maxAudioBitrate = 256_000
        )
    )
    val capabilities = provider.getCapabilities()
    val selection = selector.select(
        resolvedMedia.streams,
        MediaSelectionRequest(capabilities = capabilities)
    )

The static provider is not a substitute for querying dynamic device/player capabilities.
