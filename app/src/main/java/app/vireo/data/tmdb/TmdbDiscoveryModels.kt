package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType

enum class TmdbDiscoveryCategory {
    TRENDING_WEEK,
    POPULAR_THIS_YEAR,
    TOP_RATED,
    NEW_RELEASES,
    HIDDEN_GEMS,
    CUSTOM,
}

enum class TmdbGenreMatch {
    ANY,
    ALL,
}

enum class TmdbDiscoverySort {
    POPULARITY_DESC,
    POPULARITY_ASC,
    RATING_DESC,
    VOTE_COUNT_DESC,
    RELEASE_DATE_DESC,
}

data class TmdbDiscoveryFilters(
    val genreIds: List<Int> = emptyList(),
    val genreMatch: TmdbGenreMatch = TmdbGenreMatch.ANY,
    val minRating: Double? = null,
    val minVoteCount: Int? = null,
    val year: Int? = null,
    val decade: Int? = null,
    val originalLanguage: String? = null,
    val sortBy: TmdbDiscoverySort = TmdbDiscoverySort.POPULARITY_DESC,
) {
    val isEmpty: Boolean
        get() = genreIds.isEmpty() &&
            minRating == null &&
            minVoteCount == null &&
            year == null &&
            decade == null &&
            originalLanguage.isNullOrBlank() &&
            sortBy == TmdbDiscoverySort.POPULARITY_DESC
}

data class TmdbGenre(
    val id: Int,
    val name: String,
)

internal fun TmdbDiscoveryCategory.supportsFilters(): Boolean =
    this == TmdbDiscoveryCategory.POPULAR_THIS_YEAR ||
        this == TmdbDiscoveryCategory.NEW_RELEASES ||
        this == TmdbDiscoveryCategory.HIDDEN_GEMS ||
        this == TmdbDiscoveryCategory.CUSTOM

internal fun TmdbDiscoveryCategory.defaultSort(): TmdbDiscoverySort =
    when (this) {
        TmdbDiscoveryCategory.POPULAR_THIS_YEAR ->
            TmdbDiscoverySort.POPULARITY_DESC
        TmdbDiscoveryCategory.NEW_RELEASES ->
            TmdbDiscoverySort.RELEASE_DATE_DESC
        TmdbDiscoveryCategory.HIDDEN_GEMS ->
            TmdbDiscoverySort.POPULARITY_ASC
        else -> TmdbDiscoverySort.POPULARITY_DESC
    }
