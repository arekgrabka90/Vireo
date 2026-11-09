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
    val images: TmdbImagesDto? = null,
    val seasons: List<TmdbSeasonDto> = emptyList(),
)

@Serializable
data class TmdbSeasonDto(
    @SerialName("season_number")
    val seasonNumber: Int = 0,
    @SerialName("episode_count")
    val episodeCount: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
)

@Serializable
data class TmdbSeasonDetailsDto(
    val id: Int = 0,
    @SerialName("season_number")
    val seasonNumber: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    val episodes: List<TmdbEpisodeDto> = emptyList(),
)

@Serializable
data class TmdbEpisodeDto(
    val id: Int = 0,
    @SerialName("episode_number")
    val episodeNumber: Int = 0,
    @SerialName("season_number")
    val seasonNumber: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("still_path")
    val stillPath: String? = null,
    val runtime: Int? = null,
    @SerialName("air_date")
    val airDate: String? = null,
)

data class TmdbSeasonSummary(
    val seasonNumber: Int,
    val name: String?,
    val episodeCount: Int,
    val posterUrl: String?,
)

data class TmdbEpisodeItem(
    val tmdbEpisodeId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String?,
    val stillUrl: String?,
    val runtimeMinutes: Int?,
    val airDate: String?,
)

@Serializable
data class TmdbImagesDto(
    val logos: List<TmdbLogoDto> = emptyList(),
)

@Serializable
data class TmdbLogoDto(
    @SerialName("file_path")
    val filePath: String? = null,
    @SerialName("iso_639_1")
    val iso6391: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    @SerialName("vote_count")
    val voteCount: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
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
    val seasons: List<TmdbSeasonSummary> = emptyList(),
    val logoUrl: String? = null,
)
