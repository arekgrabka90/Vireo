package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TmdbDiscoveryQueryBuilderTest {

    private val today = LocalDate.of(2026, 11, 10)

    @Test
    fun trendingAndTopRatedRejectAdditionalFilters() {
        val filters = TmdbDiscoveryFilters(minRating = 7.0)

        listOf(
            TmdbDiscoveryCategory.TRENDING_WEEK,
            TmdbDiscoveryCategory.TOP_RATED,
        ).forEach { category ->
            try {
                TmdbDiscoveryQueryBuilder.build(
                    category, ContentType.MOVIE, filters, today
                )
                fail("Expected unsupported filter validation.")
            } catch (_: IllegalArgumentException) {
                // Expected: official ranking endpoints do not accept discover filters.
            }
        }
    }

    @Test
    fun popularThisYearUsesCorrectDateFieldForMovieAndSeries() {
        val movie = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.POPULAR_THIS_YEAR,
            ContentType.MOVIE,
            today = today,
        )
        val series = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.POPULAR_THIS_YEAR,
            ContentType.SERIES,
            today = today,
        )

        assertEquals("2026-01-01", movie["primary_release_date.gte"])
        assertEquals("2026-12-31", movie["primary_release_date.lte"])
        assertEquals("2026-01-01", series["first_air_date.gte"])
        assertEquals("2026-12-31", series["first_air_date.lte"])
        assertEquals("popularity.desc", movie["sort_by"])
    }

    @Test
    fun newReleasesUsesLastNinetyDaysAndDoesNotIncludeFutureDates() {
        val params = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.NEW_RELEASES,
            ContentType.MOVIE,
            today = today,
        )

        assertEquals("2026-08-12", params["primary_release_date.gte"])
        assertEquals("2026-11-10", params["primary_release_date.lte"])
        assertEquals("primary_release_date.desc", params["sort_by"])
    }

    @Test
    fun genreAnyAndAllUseDifferentSeparators() {
        val any = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.CUSTOM,
            ContentType.MOVIE,
            TmdbDiscoveryFilters(
                genreIds = listOf(18, 53),
                genreMatch = TmdbGenreMatch.ANY,
            ),
            today,
        )
        val all = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.CUSTOM,
            ContentType.MOVIE,
            TmdbDiscoveryFilters(
                genreIds = listOf(53, 18, 18),
                genreMatch = TmdbGenreMatch.ALL,
            ),
            today,
        )

        assertEquals("18,53", any["with_genres"])
        assertEquals("18|53", all["with_genres"])
    }

    @Test
    fun hiddenGemsEnforcesRatingVoteAndPopularityBounds() {
        val params = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.HIDDEN_GEMS,
            ContentType.MOVIE,
            today = today,
        )

        assertEquals("7.0", params["vote_average.gte"])
        assertEquals("50", params["vote_count.gte"])
        assertEquals("1000", params["vote_count.lte"])
        assertEquals("popularity.asc", params["sort_by"])
    }

    @Test
    fun customFiltersKeepOriginalLanguageSeparate() {
        val params = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.CUSTOM,
            ContentType.SERIES,
            TmdbDiscoveryFilters(
                minRating = 7.5,
                minVoteCount = 120,
                year = 2020,
                originalLanguage = "JA",
                sortBy = TmdbDiscoverySort.RATING_DESC,
            ),
            today,
        )

        assertEquals("7.5", params["vote_average.gte"])
        assertEquals("120", params["vote_count.gte"])
        assertEquals("2020-01-01", params["first_air_date.gte"])
        assertEquals("2020-12-31", params["first_air_date.lte"])
        assertEquals("ja", params["with_original_language"])
        assertEquals("vote_average.desc", params["sort_by"])
        assertFalse(params.containsKey("language"))
    }

    @Test
    fun decadeCreatesTenYearRange() {
        val params = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.CUSTOM,
            ContentType.MOVIE,
            TmdbDiscoveryFilters(decade = 1990),
            today,
        )

        assertEquals("1990-01-01", params["primary_release_date.gte"])
        assertEquals("1999-12-31", params["primary_release_date.lte"])
    }

    @Test
    fun incompatibleDateRangesAreRejected() {
        try {
            TmdbDiscoveryQueryBuilder.build(
                TmdbDiscoveryCategory.POPULAR_THIS_YEAR,
                ContentType.MOVIE,
                TmdbDiscoveryFilters(year = 2020),
                today,
            )
            fail("Expected conflicting date ranges to be rejected.")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun customQueryExcludesAdultAndVideoByDefault() {
        val params = TmdbDiscoveryQueryBuilder.build(
            TmdbDiscoveryCategory.CUSTOM,
            ContentType.MOVIE,
            today = today,
        )

        assertEquals("false", params["include_adult"])
        assertEquals("false", params["include_video"])
        assertTrue(params.containsKey("sort_by"))
    }
}
