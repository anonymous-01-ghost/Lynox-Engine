# Networking and retry policy

Lynox Engine uses the Ktor `HttpClient` supplied by the host application. The app chooses the platform engine (Android, Darwin/iOS or CIO/JVM), owns the client lifecycle, and should configure connection and request timeouts appropriate to its product.

## Transient HTTP retries

Both official YouTube Data API adapters use the shared internal retry helper:

- At most **3 total attempts** per request.
- Retries only HTTP **429** and **5xx** responses.
- Uses bounded exponential delays starting at 250 ms, capped at 2 seconds.
- Honors a numeric `Retry-After` response header, also capped at 2 seconds.
- Does not retry validation, authentication, permission, quota (YouTube commonly reports quota exhaustion as 403), or other non-transient client errors.
- Does not catch or retry coroutine cancellation. Transport exceptions are returned through the provider's normal network-error result rather than being retried. The retry helper does not classify or retry connection failures, because transport exceptions vary across the supported KMP targets.

The retry helper is intentionally internal; applications do not need to configure it. A host app may additionally configure Ktor timeouts and logging. Avoid logging full request URLs because the API key is passed as a query parameter by the upstream API.

## Client lifecycle

Create one appropriately configured `HttpClient` for the application/platform and inject it into the provider adapters. Close it when the owning application component is finished with it. Do not create a new client for every search request.
