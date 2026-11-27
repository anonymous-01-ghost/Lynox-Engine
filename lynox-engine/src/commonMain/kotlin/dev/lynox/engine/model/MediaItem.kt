package dev.lynox.engine.model

import kotlinx.serialization.Serializable

/** Provider-neutral metadata. Fields unavailable from a provider remain null. */
@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val channelId: String? = null,
    val channelTitle: String? = null,
    val durationSeconds: Long? = null,
    val thumbnailUrl: String? = null,
    val webUrl: String? = null
)
