package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPageDto(
    val page: Int = 1,
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerialName("total_results")
    val totalResults: Int = 0,
    val results: List<TmdbCatalogDto> = emptyList(),
)

@Serializable
data class TmdbCatalogDto(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("original_name")
    val originalName: String? = null,
    val overview: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("vote_count")
    val voteCount: Int = 0,
    @SerialName("genre_ids")
    val genreIds: List<Int> = emptyList(),
    @SerialName("original_language")
    val originalLanguage: String? = null,
    val popularity: Double = 0.0,
)

data class TmdbCatalogItem(
    val tmdbId: Int,
    val type: ContentType,
    val title: String,
    val originalTitle: String?,
    val releaseDate: String?,
    val year: Int?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double?,
    val voteCount: Int,
    val genreIds: List<Int>,
    val originalLanguage: String?,
    val popularity: Double,
)

data class TmdbCatalogPage(
    val items: List<TmdbCatalogItem>,
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
) {
    val hasMore: Boolean
        get() = page < totalPages
}
