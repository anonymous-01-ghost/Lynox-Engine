# Networking and retry policy

Lynox Engine is an in-process library. The core does not require a hosted backend or a Google Cloud API. Source adapters may use an application-owned Ktor HttpClient when needed; the adapter must document its network requirements and source-specific behavior.

## Legacy YouTube metadata adapters

The existing optional YouTube Data API adapters use the shared retry helper:
- At most 3 total attempts per request.
- Retries only HTTP 429 and 5xx responses.
- Bounded exponential delay starting at 250 ms and capped at 2 seconds.
- Honors numeric Retry-After values, also capped at 2 seconds.
- Does not retry validation, authentication, permission, quota, or other non-transient client errors.
- Does not catch or retry coroutine cancellation.
- Transport exceptions are returned through the provider's normal network-error result rather than retried.

This policy belongs to the legacy metadata adapters. It must not be assumed to fit every source resolver.

## Resolver networking

Each MediaResolver implementation owns source-specific redirect handling, request headers, parser behavior, retry policy, stream URL expiration and refresh. Use bounded timeouts, avoid logging credentials/cookies/signed URLs, and do not cache temporary stream URLs as ordinary metadata.
