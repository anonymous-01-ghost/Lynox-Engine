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
