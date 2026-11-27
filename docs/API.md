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

The contract does not include a built-in source adapter. A source-specific implementation must be added and tested before media extraction is available for that source.

## Search and catalog contracts

SearchProvider, MetadataProvider and CatalogProvider remain provider-neutral extension points. The repository no longer includes an official YouTube Data API adapter. These contracts do not imply that an arbitrary source supports search, channels, playlists, or metadata.

## Results and errors

Operations use EngineResult.Success or EngineResult.Failure. Expected operational errors are represented by EngineError.

## Stability

Names and signatures may change before version 1.0. Every new source adapter must include parser fixtures, failure cases, and platform integration tests before being called production-ready.
