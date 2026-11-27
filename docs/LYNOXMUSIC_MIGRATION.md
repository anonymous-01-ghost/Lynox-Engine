# LynoxMusic migration target

This document records the integration surface that Lynox Engine must eventually replace. It is a migration specification, not a claim that the extractor is already implemented.

## Current LynoxMusic integration

The LynoxMusic Android app currently declares NewPipe Extractor v0.27.0 and uses it from YouTubeAudio.kt for:
- YouTube video search and pagination;
- video metadata (title, uploader, thumbnail and duration);
- audio stream resolution;
- selection of progressive M4A audio renditions by bitrate/quality;
- short-lived in-memory caching of a recently resolved audio URL.

The existing app also hands the resolved audio URL to AndroidX Media3. Lynox Engine must keep extraction and playback separate: the library resolves and describes renditions; LynoxMusic's player owns playback.

## Required Lynox Engine capability mapping

| Existing operation | Lynox Engine target API |
|---|---|
| Search | SearchProvider / LynoxEngine.search(SearchRequest) |
| Load track metadata | MetadataProvider / LynoxEngine.getMediaInfo(mediaId) |
| Resolve audio/video renditions | MediaResolver / LynoxEngine.resolveMedia(sourceUrl) |
| Quality selection | Select a returned audio rendition using bitrate, MIME type and codecs |
| Pagination | SearchPage continuation token |
| Expiring URL refresh | Re-run resolveMedia; do not persist stream URLs |

## Compatibility requirements

A future adapter must preserve:
- stable source IDs so the app can keep its stored track identifiers;
- title, uploader/channel, thumbnail and duration metadata;
- separate audio/video rendition descriptors;
- MIME type, codecs, bitrate, delivery method and expiry where available;
- clear errors for unavailable, private, age-restricted, deleted or unsupported media;
- bounded network timeouts and cancellation propagation.

The core should not embed Android Media3 classes. The Android app converts a selected MediaStream into its Media3 MediaItem. iOS and desktop provide their own playback adapters.

## Migration rule

Do not remove NewPipe Extractor from LynoxMusic until a Lynox Engine source adapter passes parity tests for search, metadata, pagination, audio rendition selection, expiry/refresh and real playback on supported devices. This avoids breaking the current player while the replacement is incomplete.

## Source-access boundary

The official YouTube Data API does not return raw media streams and is not a substitute for NewPipe Extractor. Any YouTube-specific implementation must be separately reviewed against the source's current terms and access restrictions. This migration document does not authorize DRM circumvention or bypassing authentication or other access controls.
