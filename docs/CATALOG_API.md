# Catalog contracts

Lynox Engine is an in-process Kotlin Multiplatform library. The catalog package defines provider-neutral models and an optional capability contract; it does not ship a source-specific catalog adapter.

## Public models and provider contract

The dev.lynox.engine.catalog package defines:
- Channel: normalized channel metadata.
- Playlist: normalized playlist metadata.
- PlaylistEntry: a media reference within a playlist.
- CatalogRequest: page size, continuation token and optional search query.
- CatalogProvider: optional capability contract for catalog operations.

A source adapter may implement CatalogProvider only when the source exposes these capabilities through permitted access methods. Fields that are unavailable must remain null; the core must not invent metadata.

## Boundary

The catalog contract is independent of media extraction. Search and metadata do not automatically provide playable media streams. The core has no Google Cloud API-key or quota dependency.

## Provider implementation rules

- Return `EngineResult.Success` only when a complete, valid response is available.
- Represent expected upstream failures with `EngineResult.Failure`, using the most specific `EngineError` available.
- Never swallow coroutine cancellation or convert cancellation into a cacheable result; allow cancellation exceptions to propagate.
- Treat continuation tokens as opaque values. Do not parse, trim, or synthesize them unless the upstream protocol explicitly requires that transformation.
- The engine trims search queries and resource identifiers, validates page sizes (1–50), and caches successful responses only. Failed results are requested again on the next call.
- Do not invent fields that the source does not provide. Keep unavailable optional metadata null.
