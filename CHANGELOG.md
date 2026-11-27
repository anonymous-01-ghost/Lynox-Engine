# Changelog

All notable changes to Lynox Engine will be documented here.

## Unreleased

- Corrected the product direction: Lynox Engine is an independent, in-process media extraction library, not a Google Cloud or YouTube Data API wrapper.
- Removed the YouTube Data API provider implementations and their tests from the core project.
- Removed the obsolete Ktor retry helper and transport dependencies; the core is now transport-neutral.
- Added source-neutral MediaResolver, ResolvedMedia, MediaStream and MediaStreamKind contracts, exposed through LynoxEngine.resolveMedia().
- Added URL validation and tests for resolver injection, normalized URLs and unconfigured providers.
- Documented the LynoxMusic migration target and retained NewPipe in the app until a real replacement passes parity tests.
- Updated architecture, build dependencies, integration and compliance documentation.
