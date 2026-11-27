# Networking strategy

Lynox Engine uses Ktor Client in shared code and selects a platform engine per target:

- Android: Ktor Android engine.
- iOS: Ktor Darwin engine.
- JVM desktop: Ktor CIO engine.

The consuming application creates and owns the `HttpClient`, then injects it into provider adapters. The application is responsible for closing the client when its lifecycle ends. This avoids hidden global clients and lets applications configure timeouts, logging, TLS and proxies.

## YouTube Data API v3 adapter

`YouTubeDataApiProvider` uses the documented HTTPS endpoints:

- `search.list` for video search and pagination.
- `videos.list` for public video metadata and duration.

The adapter accepts an API key at construction. Never commit keys to the repository or embed an unrestricted key in a distributed app. Restrict the key in Google Cloud, monitor quota usage, and consider a trusted backend if the key cannot be protected in the client.

## Reliability

- Requests are cancellable.
- Coroutine cancellation is propagated and is not converted into a network error.
- HTTP errors are mapped to typed engine errors.
- Authentication, invalid requests and quota errors must not be retried blindly.
- Add bounded retries only for transient failures and honor provider retry guidance.

This adapter provides metadata/search only. It does not return playable media streams or implement a player.
