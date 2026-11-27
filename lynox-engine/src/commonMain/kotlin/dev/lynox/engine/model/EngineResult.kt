package dev.lynox.engine.model

sealed interface EngineResult<out T> {
    data class Success<T>(val value: T) : EngineResult<T>
    data class Failure(val error: EngineError) : EngineResult<Nothing>
}

sealed interface EngineError {
    data object ProviderNotConfigured : EngineError
    data class InvalidRequest(val reason: String) : EngineError
    data class Network(val message: String? = null) : EngineError
    data class QuotaExceeded(val retryAfterSeconds: Long? = null) : EngineError
    data class Unauthorized(val message: String? = null) : EngineError
    data class ProviderFailure(val code: String, val message: String? = null) : EngineError
    data object Unknown : EngineError
}
