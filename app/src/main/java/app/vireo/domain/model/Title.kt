package app.vireo.domain.model

data class Title(
    val id: String,
    val name: String,
    val posterUrl: String?,
    val rating: String?,
    val type: ContentType,
    val categoryId: String?,
    val epgChannelId: String? = null,
    val logoUrl: String? = null,
)
