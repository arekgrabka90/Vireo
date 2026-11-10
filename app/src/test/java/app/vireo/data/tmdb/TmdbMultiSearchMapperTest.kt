package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbMultiSearchMapperTest {

    @Test
    fun movieResultMapsToMovie() {
        val mapped = TmdbMultiSearchMapper.map(
            TmdbMultiSearchResultDto(
                id = 157336,
                mediaType = "movie",
                title = "Interstellar",
                releaseDate = "2014-11-05",
            )
        )
        assertNotNull(mapped)
        val item = (mapped as TmdbMultiSearchMapped.Media).item
        assertEquals(ContentType.MOVIE, item.type)
        assertEquals("Interstellar", item.title)
        assertEquals(2014, item.year)
    }

    @Test
    fun tvResultMapsToSeries() {
        val mapped = TmdbMultiSearchMapper.map(
            TmdbMultiSearchResultDto(
                id = 1399,
                mediaType = "tv",
                name = "Game of Thrones",
                firstAirDate = "2011-04-17",
            )
        )
        val item = (mapped as TmdbMultiSearchMapped.Media).item
        assertEquals(ContentType.SERIES, item.type)
        assertEquals(2011, item.year)
    }

    @Test
    fun personMapsKnownProductions() {
        val mapped = TmdbMultiSearchMapper.map(
            TmdbMultiSearchResultDto(
                id = 1,
                mediaType = "person",
                name = "Matthew McConaughey",
                profilePath = "/person.jpg",
                knownFor = listOf(
                    TmdbMultiSearchResultDto(
                        id = 157336,
                        mediaType = "movie",
                        title = "Interstellar",
                    )
                ),
            )
        )
        val person = (mapped as TmdbMultiSearchMapped.Person).item
        assertEquals("Matthew McConaughey", person.name)
        assertEquals(1, person.knownFor.size)
        assertEquals("Interstellar", person.knownFor.single().title)
    }

    @Test
    fun unsupportedOrInvalidEntriesAreIgnored() {
        assertNull(
            TmdbMultiSearchMapper.map(
                TmdbMultiSearchResultDto(id = 10, mediaType = "collection")
            )
        )
        assertNull(
            TmdbMultiSearchMapper.map(
                TmdbMultiSearchResultDto(id = 0, mediaType = "person", name = "Invalid")
            )
        )
    }
}
