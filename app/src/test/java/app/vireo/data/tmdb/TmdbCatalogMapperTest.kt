package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbCatalogMapperTest {

    @Test
    fun `movie maps title date rating and artwork`() {
        val result = TmdbCatalogMapper.map(
            TmdbCatalogDto(
                id = 157336,
                title = "Interstellar",
                originalTitle = "Interstellar",
                releaseDate = "2014-11-05",
                posterPath = "/poster.jpg",
                backdropPath = "/backdrop.jpg",
                voteAverage = 8.6,
                voteCount = 1000,
                genreIds = listOf(18, 878),
                originalLanguage = "en",
            ),
            ContentType.MOVIE,
        )

        assertEquals(157336, result?.tmdbId)
        assertEquals("Interstellar", result?.title)
        assertEquals(2014, result?.year)
        assertEquals(ContentType.MOVIE, result?.type)
        assertEquals(8.6, result?.rating ?: 0.0, 0.001)
        assertEquals(
            "https://image.tmdb.org/t/p/w500/poster.jpg",
            result?.posterUrl,
        )
        assertEquals(
            "https://image.tmdb.org/t/p/w780/backdrop.jpg",
            result?.backdropUrl,
        )
    }

    @Test
    fun `series uses name and first air date`() {
        val result = TmdbCatalogMapper.map(
            TmdbCatalogDto(
                id = 1399,
                name = "Game of Thrones",
                originalName = "Game of Thrones",
                firstAirDate = "2011-04-17",
            ),
            ContentType.SERIES,
        )

        assertEquals("Game of Thrones", result?.title)
        assertEquals(2011, result?.year)
        assertEquals(ContentType.SERIES, result?.type)
    }

    @Test
    fun `invalid identifiers and empty titles are ignored`() {
        assertNull(
            TmdbCatalogMapper.map(
                TmdbCatalogDto(id = 0, title = "Invalid"),
                ContentType.MOVIE,
            )
        )

        assertNull(
            TmdbCatalogMapper.map(
                TmdbCatalogDto(id = 10, title = " "),
                ContentType.MOVIE,
            )
        )
    }

    @Test
    fun `live tv is not accepted as a tmdb catalog type`() {
        assertNull(
            TmdbCatalogMapper.map(
                TmdbCatalogDto(id = 10, title = "Not Live TV"),
                ContentType.LIVE,
            )
        )
    }

    @Test
    fun `page has more when current page is below total pages`() {
        val page = TmdbCatalogPage(
            items = emptyList(),
            page = 1,
            totalPages = 5,
            totalResults = 100,
        )

        assertTrue(page.hasMore)
        assertFalse(page.copy(page = 5).hasMore)
    }
}
