package app.vireo.ui.search

import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbMultiSearchResult
import app.vireo.data.tmdb.TmdbPersonSearchItem
import app.vireo.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchPagingRulesTest {

    private fun media(id: Int) = TmdbCatalogItem(
        tmdbId = id,
        type = ContentType.MOVIE,
        title = "Movie $id",
        originalTitle = null,
        releaseDate = null,
        year = null,
        overview = null,
        posterUrl = null,
        backdropUrl = null,
        rating = null,
        voteCount = 0,
        genreIds = emptyList(),
        originalLanguage = null,
        popularity = 1.0,
    )

    private fun person(id: Int, popularity: Double) = TmdbPersonSearchItem(
        tmdbId = id,
        name = "Person $id",
        profileUrl = null,
        popularity = popularity,
        knownFor = emptyList(),
    )

    @Test
    fun appendsPagesWithoutDuplicateMediaOrPeople() {
        val first = TmdbMultiSearchResult(
            media = listOf(media(1), media(2)),
            people = listOf(person(10, 1.0)),
            page = 1,
            totalPages = 3,
            totalResults = 60,
        )
        val second = TmdbMultiSearchResult(
            media = listOf(media(2), media(3)),
            people = listOf(person(10, 1.0), person(11, 2.0)),
            page = 2,
            totalPages = 3,
            totalResults = 60,
        )

        val merged = mergeSearchResults(first, second)

        assertEquals(listOf(1, 2, 3), merged.media.map { it.tmdbId })
        assertEquals(listOf(11, 10), merged.people.map { it.tmdbId })
        assertEquals(2, merged.page)
        assertEquals(3, merged.totalPages)
        assertEquals(60, merged.totalResults)
    }

    @Test
    fun keepsSeparateMovieAndSeriesIds() {
        val movie = media(42)
        val series = movie.copy(type = ContentType.SERIES)

        val merged = mergeSearchResults(
            TmdbMultiSearchResult(listOf(movie), emptyList()),
            TmdbMultiSearchResult(listOf(series), emptyList(), page = 2),
        )

        assertEquals(2, merged.media.size)
    }
}
