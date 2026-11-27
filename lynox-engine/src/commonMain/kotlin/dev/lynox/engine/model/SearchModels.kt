package dev.lynox.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val query: String,
    val pageToken: String? = null,
    val pageSize: Int = 20
) {
    init {
        require(pageSize in 1..50) { "pageSize must be between 1 and 50" }
    }
}

@Serializable
data class SearchPage<T>(
    val items: List<T>,
    val nextPageToken: String? = null
)
