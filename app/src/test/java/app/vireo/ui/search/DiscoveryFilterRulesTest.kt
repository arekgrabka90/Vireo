package app.vireo.ui.search

import app.vireo.data.tmdb.TmdbDiscoveryFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveryFilterRulesTest {

    @Test
    fun selectingYearClearsDecade() {
        val result = TmdbDiscoveryFilters(decade = 1990).withYear(1997)

        assertEquals(1997, result.year)
        assertNull(result.decade)
    }

    @Test
    fun selectingDecadeClearsYear() {
        val result = TmdbDiscoveryFilters(year = 2004).withDecade(1990)

        assertEquals(1990, result.decade)
        assertNull(result.year)
    }

    @Test
    fun genresAreDistinctAndSorted() {
        val result = TmdbDiscoveryFilters().withGenres(listOf(18, 28, 18))

        assertEquals(listOf(18, 28), result.genreIds)
    }

    @Test
    fun originalLanguageIsTrimmedAndLowercase() {
        val result = TmdbDiscoveryFilters().withOriginalLanguage(" JA ")

        assertEquals("ja", result.originalLanguage)
    }

    @Test
    fun blankOriginalLanguageBecomesNull() {
        val result = TmdbDiscoveryFilters().withOriginalLanguage("  ")

        assertNull(result.originalLanguage)
    }
}
