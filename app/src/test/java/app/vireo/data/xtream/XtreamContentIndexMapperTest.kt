package app.vireo.data.xtream

import app.vireo.domain.model.Movie
import app.vireo.domain.model.Series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XtreamContentIndexMapperTest {

    @Test
    fun `movie index keeps account identity and normalized metadata`() {
        val result = XtreamContentIndexMapper.movie(
            sourceId = 7L,
            item = Movie(
                id = "42",
                title = "Dune (2021)",
                posterUrl = "https://example.test/poster.jpg",
                categoryId = "5",
                rating = "8.1",
                streamUrl = "https://example.test/movie/user/password/42.mp4",
            ),
            indexedAt = 1234L,
        )

        assertEquals(7L, result?.sourceId)
        assertEquals("MOVIE", result?.contentType)
        assertEquals("42", result?.streamId)
        assertEquals("dune", result?.normalizedTitle)
        assertEquals(2021, result?.year)
        assertEquals(1234L, result?.indexedAt)
    }

    @Test
    fun `series index uses separate content type`() {
        val result = XtreamContentIndexMapper.series(
            sourceId = 9L,
            item = Series(
                id = "77",
                title = "Dark (2017)",
                posterUrl = null,
                categoryId = "2",
                rating = "8.7",
            ),
            indexedAt = 5678L,
        )

        assertEquals(9L, result?.sourceId)
        assertEquals("SERIES", result?.contentType)
        assertEquals("77", result?.streamId)
        assertEquals("dark", result?.normalizedTitle)
        assertEquals(2017, result?.year)
    }

    @Test
    fun `invalid source identifiers and blank titles are ignored`() {
        val invalidSource = XtreamContentIndexMapper.movie(
            sourceId = 0L,
            item = Movie("1", "Title", null, null, null, "url"),
            indexedAt = 1L,
        )

        val blankTitle = XtreamContentIndexMapper.movie(
            sourceId = 1L,
            item = Movie("1", "   ", null, null, null, "url"),
            indexedAt = 1L,
        )

        assertNull(invalidSource)
        assertNull(blankTitle)
    }
}
