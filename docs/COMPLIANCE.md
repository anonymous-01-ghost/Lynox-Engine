# YouTube API compliance boundary

The initial YouTube adapter is limited to documented YouTube Data API v3 operations for search and public metadata.

## Allowed scope in this repository

- Search public videos through `search.list`.
- Retrieve public video metadata through `videos.list`.
- Normalize returned metadata into Lynox models.
- Link users to YouTube video pages or supported YouTube player experiences.

## Out of scope

- Scraping YouTube pages or undocumented/private endpoints.
- Extracting direct audio/video stream URLs.
- Circumventing advertisements, DRM, access controls, quotas or other platform restrictions.
- Downloading or caching media in ways not permitted by applicable terms.
- Claiming that Data API search results provide playable media streams.

## API key handling

Do not commit keys. Restrict keys to the required APIs and appropriate application restrictions, monitor quota, and rotate exposed keys. A client-side key may be recoverable from a distributed application; use a trusted backend when stronger key protection is required.

Review the current YouTube API Services Terms and Developer Policies before publishing or distributing an application. This document is an engineering boundary, not legal advice.
