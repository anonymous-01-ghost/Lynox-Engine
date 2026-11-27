# Public API

This document describes the pre-1.0 public API. It is not yet a stable 1.0 contract.

## Media extraction boundary

The core defines a source-neutral extraction contract:

- MediaResolver.resolve(sourceUrl) resolves a source URL.
- ResolvedMedia represents the resolved media item.
- MediaStream represents an available audio, video, or combined rendition.
- MediaStreamKind distinguishes rendition types.

A source adapter is injected by the consuming application. The contract does not contain a built-in YouTube extractor and does not claim that media streams are already resolved.

## Legacy search and metadata

The existing SearchProvider, MetadataProvider and CatalogProvider contracts remain available. Optional YouTubeDataApiProvider and YouTubeDataApiCatalogProvider implementations perform official YouTube Data API search/metadata calls only. They are not part of the extraction path and require an API key only when explicitly configured.

## Results and errors

Operations use EngineResult.Success or EngineResult.Failure. Expected operational errors are represented by EngineError.

## Stability

Names and signatures may change before version 1.0. Every new source adapter must include parser fixtures, failure cases, and platform integration tests before being called production-ready.
