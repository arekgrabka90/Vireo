package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import java.time.LocalDate

internal object TmdbDiscoveryQueryBuilder {

    fun build(
        category: TmdbDiscoveryCategory,
        type: ContentType,
        filters: TmdbDiscoveryFilters = TmdbDiscoveryFilters(),
        today: LocalDate = LocalDate.now(),
    ): Map<String, String> {
        require(type == ContentType.MOVIE || type == ContentType.SERIES) {
            "Discovery supports movies and series only."
        }
        require(category.supportsFilters() || filters.isEmpty) {
            "This Discovery category does not support additional filters."
        }
        validate(filters)

        val params = linkedMapOf<String, String>()

        if (filters.genreIds.isNotEmpty()) {
            val ids = filters.genreIds.distinct().sorted().joinToString(
                separator = if (filters.genreMatch == TmdbGenreMatch.ALL) "|" else ","
            )
            params["with_genres"] = ids
        }

        filters.minRating?.let {
            params["vote_average.gte"] = it.toString()
        }

        filters.minVoteCount?.let {
            params["vote_count.gte"] = it.toString()
        }

        filters.originalLanguage?.trim()?.lowercase()?.let {
            if (it.isNotBlank()) params["with_original_language"] = it
        }

        val dateField = if (type == ContentType.MOVIE) {
            "primary_release_date"
        } else {
            "first_air_date"
        }

        var lowerDate: LocalDate? = null
        var upperDate: LocalDate? = null

        when (category) {
            TmdbDiscoveryCategory.POPULAR_THIS_YEAR -> {
                lowerDate = today.withDayOfYear(1)
                upperDate = today.withDayOfYear(today.lengthOfYear())
            }
            TmdbDiscoveryCategory.NEW_RELEASES -> {
                lowerDate = today.minusDays(90)
                upperDate = today
            }
            else -> Unit
        }

        filters.year?.let {
            val start = LocalDate.of(it, 1, 1)
            val end = LocalDate.of(it, 12, 31)
            lowerDate = later(lowerDate, start)
            upperDate = earlier(upperDate, end)
        }

        filters.decade?.let {
            val start = LocalDate.of(it, 1, 1)
            val end = LocalDate.of(it + 9, 12, 31)
            lowerDate = later(lowerDate, start)
            upperDate = earlier(upperDate, end)
        }

        val lowerBound = lowerDate
        val upperBound = upperDate
        if (lowerBound != null && upperBound != null) {
            require(!lowerBound.isAfter(upperBound)) {
                "The selected date filters do not overlap."
            }
        }

        lowerDate?.let { params["$dateField.gte"] = it.toString() }
        upperDate?.let { params["$dateField.lte"] = it.toString() }

        when (category) {
            TmdbDiscoveryCategory.HIDDEN_GEMS -> {
                params["vote_average.gte"] = maxOf(
                    filters.minRating ?: 0.0,
                    HIDDEN_GEMS_MIN_RATING,
                ).toString()
                params["vote_count.gte"] = maxOf(
                    filters.minVoteCount ?: 0,
                    HIDDEN_GEMS_MIN_VOTES,
                ).toString()
                params["vote_count.lte"] = HIDDEN_GEMS_MAX_VOTES.toString()
                params["sort_by"] = "popularity.asc"
            }
            TmdbDiscoveryCategory.POPULAR_THIS_YEAR ->
                params["sort_by"] = "popularity.desc"
            TmdbDiscoveryCategory.NEW_RELEASES ->
                params["sort_by"] = if (type == ContentType.MOVIE) {
                    "primary_release_date.desc"
                } else {
                    "first_air_date.desc"
                }
            TmdbDiscoveryCategory.CUSTOM ->
                params["sort_by"] = filters.sortBy.toTmdbValue(type)
            else -> Unit
        }

        params["include_adult"] = "false"
        params["include_video"] = "false"
        return params
    }

    private fun validate(filters: TmdbDiscoveryFilters) {
        require(filters.genreIds.all { it > 0 }) {
            "Genre IDs must be positive."
        }
        require(filters.minRating == null ||
            (filters.minRating.isFinite() && filters.minRating in 0.0..10.0)) {
            "Minimum rating must be between 0 and 10."
        }
        require(filters.minVoteCount == null || filters.minVoteCount >= 0) {
            "Minimum vote count cannot be negative."
        }
        require(filters.year == null || filters.year in 1870..2200) {
            "Year is outside the supported range."
        }
        require(filters.decade == null ||
            (filters.decade in 1870..2190 && filters.decade % 10 == 0)) {
            "Decade must be a valid decade start year."
        }
        require(filters.year == null || filters.decade == null) {
            "Select either a year or a decade, not both."
        }
        require(filters.originalLanguage.isNullOrBlank() ||
            LANGUAGE_PATTERN.matches(filters.originalLanguage.trim().lowercase())) {
            "Original language must be a two-letter ISO language code."
        }
        require(
            filters.minVoteCount == null ||
                filters.minVoteCount <= HIDDEN_GEMS_MAX_VOTES ||
                filters.minRating == null
        ) {
            "The selected minimum vote count conflicts with Hidden Gems limits."
        }
    }

    private fun TmdbDiscoverySort.toTmdbValue(type: ContentType): String =
        when (this) {
            TmdbDiscoverySort.POPULARITY_DESC -> "popularity.desc"
            TmdbDiscoverySort.POPULARITY_ASC -> "popularity.asc"
            TmdbDiscoverySort.RATING_DESC -> "vote_average.desc"
            TmdbDiscoverySort.VOTE_COUNT_DESC -> "vote_count.desc"
            TmdbDiscoverySort.RELEASE_DATE_DESC ->
                if (type == ContentType.MOVIE) {
                    "primary_release_date.desc"
                } else {
                    "first_air_date.desc"
                }
        }

    private fun later(a: LocalDate?, b: LocalDate): LocalDate =
        if (a == null || b.isAfter(a)) b else a

    private fun earlier(a: LocalDate?, b: LocalDate): LocalDate =
        if (a == null || b.isBefore(a)) b else a

    private const val HIDDEN_GEMS_MIN_RATING = 7.0
    private const val HIDDEN_GEMS_MIN_VOTES = 50
    private const val HIDDEN_GEMS_MAX_VOTES = 1000
    private val LANGUAGE_PATTERN = Regex("^[a-z]{2}$")
}
