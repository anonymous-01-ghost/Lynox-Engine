package dev.lynox.engine.provider

import dev.lynox.engine.model.EngineError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Maps YouTube Data API error reasons without exposing upstream messages or request URLs.
 */
internal fun mapYouTubeHttpError(status: Int, body: String, json: Json): EngineError {
    val reason = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
            ?.get("errors")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("reason")?.jsonPrimitive?.content
    }.getOrNull()

    return when (status) {
        400 -> EngineError.InvalidRequest("YouTube rejected the request (HTTP 400)")
        401 -> EngineError.Unauthorized("YouTube API authentication failed")
        403 -> when (reason) {
            "quotaExceeded", "dailyLimitExceeded", "userRateLimitExceeded" ->
                EngineError.QuotaExceeded()
            "keyInvalid", "authError" ->
                EngineError.Unauthorized("YouTube API authentication failed")
            "accessNotConfigured" ->
                EngineError.ProviderFailure("API_NOT_ENABLED", "YouTube Data API v3 is not enabled for this project")
            else ->
                EngineError.Unauthorized("YouTube API access was denied")
        }
        404 -> EngineError.ProviderFailure("RESOURCE_NOT_FOUND", "The requested YouTube resource or endpoint was not found")
        else -> EngineError.ProviderFailure("HTTP_$status", "YouTube request failed (HTTP $status)")
    }
}
