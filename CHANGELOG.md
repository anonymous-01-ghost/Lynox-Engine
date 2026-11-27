# Changelog

All notable changes to Lynox Engine will be documented here.

## Unreleased

- Corrected the product direction: Lynox Engine is an independent, in-process media extraction library, not a Google Cloud or YouTube Data API wrapper.
- Added source-neutral MediaResolver, ResolvedMedia, MediaStream and MediaStreamKind contracts as the extraction foundation.
- Retained the existing YouTube Data API adapters as optional legacy search/metadata functionality; they are not an extractor.
- Updated architecture, API, integration and compliance documentation to distinguish implemented contracts from unfinished source adapters.
