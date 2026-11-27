# Public API

This document describes the initial API surface. It is not yet a stable 1.0 contract.

## Entry point

`LynoxEngine` receives optional provider implementations through its constructor.

## Search

`SearchRequest` contains:
- `query`: required non-blank search text;
- `pageToken`: optional continuation token;
- `pageSize`: 1–50, default 20.

`SearchProvider` returns `EngineResult<SearchPage<MediaItem>>`.

## Metadata

`MetadataProvider.getMediaInfo(mediaId)` returns a normalized `MediaItem` or typed error.

## Results and errors

Operations return `EngineResult.Success` or `EngineResult.Failure`. Expected operational errors are represented by `EngineError`; callers should not parse provider-specific exception messages.

## Stability

Names and signatures may change before version 1.0. New public APIs must include KDoc and tests.
