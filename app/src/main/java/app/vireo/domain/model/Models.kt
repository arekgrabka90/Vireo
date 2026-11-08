package app.vireo.domain.model

enum class ContentType { LIVE, MOVIE, SERIES }

data class Category(val id: String, val name: String, val type: ContentType)

data class Channel(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val categoryId: String?,
    val epgChannelId: String?,
    val streamUrl: String,
)

data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String?,
    val rating: String?,
    val streamUrl: String,
    val tmdbId: Int? = null,
    val imdbId: String? = null,
)

data class Series(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String?,
    val rating: String?,
    val tmdbId: Int? = null,
    val imdbId: String? = null,
)

data class AccountInfo(
    val status: String,
    val expiresAt: Long?,
    val maxConnections: Int?,
)
