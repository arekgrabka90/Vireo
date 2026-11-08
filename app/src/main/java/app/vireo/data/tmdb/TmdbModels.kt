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
    val imdbId: String? = null,
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

@Serializable
data class TmdbDetailDto(
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
    val genres: List<TmdbGenreDto> = emptyList(),
    val runtime: Int? = null,
    @SerialName("episode_run_time")
    val episodeRunTime: List<Int> = emptyList(),
    val credits: TmdbCreditsDto? = null,
    @SerialName("external_ids")
    val externalIds: TmdbExternalIdsDto? = null,
)

@Serializable
data class TmdbExternalIdsDto(
    @SerialName("imdb_id")
    val imdbId: String? = null,
)

@Serializable
data class TmdbGenreDto(
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class TmdbCreditsDto(
    val cast: List<TmdbCastDto> = emptyList(),
)

@Serializable
data class TmdbCastDto(
    val id: Int = 0,
    val name: String = "",
    val character: String = "",
    @SerialName("profile_path")
    val profilePath: String? = null,
)

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profileUrl: String?,
)

data class TmdbDetail(
    val tmdbId: Int,
    val type: ContentType,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val releaseDate: String?,
    val year: Int?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double?,
    val voteCount: Int,
    val genres: List<String>,
    val runtimeMinutes: Int?,
    val cast: List<TmdbCastMember>,
    val imdbId: String? = null,
)
