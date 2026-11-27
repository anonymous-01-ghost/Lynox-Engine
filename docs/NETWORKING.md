# Networking boundary

The Lynox Engine core is transport-neutral. It does not bundle Ktor, an HTTP engine, a hosted backend, or a source API client.

Each source adapter chooses and owns the networking implementation appropriate for its platform and source. It must document:
- redirect and request-header behavior;
- connection/request timeouts;
- bounded retry policy for transient failures;
- cancellation propagation;
- parser and malformed-response handling;
- stream URL expiration and refresh;
- sensitive-data redaction in logs.

Do not log authorization headers, cookies, tokens, or signed media URLs. Do not cache temporary stream URLs as ordinary metadata.

A consuming application may use Ktor, OkHttp, URLSession, or another suitable client in its source adapter. The core public contract does not force one networking stack.
