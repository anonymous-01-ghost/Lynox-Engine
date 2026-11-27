# Source access and compliance boundary

Lynox Engine is designed as a client-side library, not a hosted API. The core must not require Google Cloud, YouTube Data API credentials, a Lynox backend, or AI tokens.

## Source adapters

Each source adapter must document:
- the source and supported URL types;
- the access method and its applicable terms;
- authentication and permission requirements;
- whether direct media resolution is permitted;
- stream URL expiration and refresh behavior;
- supported platforms and playback constraints.

Use only media that the application is authorized to access and resolve. Do not bypass DRM, authentication, paywalls, private access controls, or service restrictions. Do not misrepresent an official metadata API as a media extractor.

## YouTube-specific boundary

The official YouTube Data API supports search and public metadata; it does not provide raw audio/video streams. Its adapter remains optional legacy functionality and is not the Lynox extraction engine.

For YouTube playback, use supported YouTube playback integrations and current developer policies. This repository does not claim that its generic resolver contract authorizes direct extraction of YouTube media streams.

## Credential handling

Never commit credentials. The core extraction contract does not require a Google API key. If an optional provider uses credentials, document their scope and the risk of embedding client-side keys.

This document is an engineering boundary, not legal advice. Review the source's current terms and applicable law before shipping an adapter.
