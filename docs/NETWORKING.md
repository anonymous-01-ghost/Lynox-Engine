# Networking and retry policy

Lynox Engine is an in-process library. The core does not require a hosted backend or Google Cloud API. Source adapters may use an application-owned Ktor HttpClient when needed; each adapter must document its network requirements and source-specific behavior.

## Shared retry helper

The repository includes a bounded internal HTTP retry helper:
- At most 3 total attempts per request.
- Retries only HTTP 429 and 5xx responses.
- Bounded exponential delay starting at 250 ms and capped at 2 seconds.
- Honors numeric Retry-After values, also capped at 2 seconds.
- Does not retry validation, authentication, permission, quota, or other non-transient client errors.
- Does not catch or retry coroutine cancellation.
- Transport exceptions are not retried because their classification differs across supported KMP targets.

A source adapter must decide whether this policy is appropriate for its source; do not assume the helper fits every protocol.

## Resolver networking

Each MediaResolver implementation owns source-specific redirect handling, request headers, parser behavior, retry policy, stream URL expiration and refresh. Use bounded timeouts, avoid logging credentials/cookies/signed URLs, and do not cache temporary stream URLs as ordinary metadata.
