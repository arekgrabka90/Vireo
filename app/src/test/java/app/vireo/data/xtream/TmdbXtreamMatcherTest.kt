package app.vireo.data.xtream

import app.vireo.data.local.XtreamContentIndexEntity
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbXtreamMatcherTest {

    private fun tmdb(
        title: String = "Dune",
        originalTitle: String? = "Dune",
        year: Int? = 2021,
        type: ContentType = ContentType.MOVIE,
    ) = TmdbCatalogItem(
        tmdbId = 123,
        type = type,
        title = title,
        originalTitle = originalTitle,
        releaseDate = year?.let { "$it-01-01" },
        year = year,
        overview = null,
        posterUrl = null,
        backdropUrl = null,
        rating = null,
        voteCount = 0,
        genreIds = emptyList(),
        originalLanguage = null,
        popularity = 0.0,
    )

    private fun indexed(
        sourceId: Long = 1,
        streamId: String = "10",
        title: String = "Dune (2021)",
        normalizedTitle: String = "dune",
        year: Int? = 2021,
        type: String = "MOVIE",
    ) = XtreamContentIndexEntity(
        sourceId = sourceId,
        contentType = type,
        streamId = streamId,
        title = title,
        normalizedTitle = normalizedTitle,
        year = year,
        posterUrl = null,
        categoryId = null,
        indexedAt = 1000L,
    )

    @Test
    fun `exact title and year produce confirmed match`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(),
            listOf(indexed()),
        )

        assertEquals(TmdbXtreamMatchStatus.MATCHED, result.status)
        assertEquals(1, result.variants.size)
        assertEquals(
            TmdbXtreamMatchConfidence.EXACT_TITLE,
            result.variants.first().confidence,
        )
    }

    @Test
    fun `original title and matching year produce confirmed variant`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(
                title = "Obcy",
                originalTitle = "Alien",
                year = 1979,
            ),
            listOf(
                indexed(
                    title = "Alien (1979)",
                    normalizedTitle = "alien",
                    year = 1979,
                )
            ),
        )

        assertEquals(TmdbXtreamMatchStatus.MATCHED, result.status)
        assertEquals(
            TmdbXtreamMatchConfidence.ORIGINAL_TITLE,
            result.variants.first().confidence,
        )
    }

    @Test
    fun `different release year rejects candidate`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(year = 2021),
            listOf(indexed(year = 1984)),
        )

        assertEquals(TmdbXtreamMatchStatus.NOT_FOUND, result.status)
        assertTrue(result.variants.isEmpty())
    }

    @Test
    fun `missing candidate year requires review`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(year = 2021),
            listOf(indexed(year = null)),
        )

        assertEquals(TmdbXtreamMatchStatus.NEEDS_REVIEW, result.status)
        assertEquals(
            TmdbXtreamMatchConfidence.NEEDS_REVIEW,
            result.variants.first().confidence,
        )
    }

    @Test
    fun `missing TMDB year requires review`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(year = null),
            listOf(indexed(year = 2021)),
        )

        assertEquals(TmdbXtreamMatchStatus.NEEDS_REVIEW, result.status)
    }

    @Test
    fun `variants from all accounts are preserved`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(),
            listOf(
                indexed(sourceId = 1, streamId = "10"),
                indexed(sourceId = 2, streamId = "20"),
                indexed(sourceId = 3, streamId = "30"),
            ),
        )

        assertEquals(TmdbXtreamMatchStatus.MATCHED, result.status)
        assertEquals(3, result.variants.size)
        assertEquals(
            setOf(1L, 2L, 3L),
            result.variants.map { it.sourceId }.toSet(),
        )
    }

    @Test
    fun `wrong content type is never matched`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(type = ContentType.MOVIE),
            listOf(indexed(type = "SERIES")),
        )

        assertEquals(TmdbXtreamMatchStatus.NOT_FOUND, result.status)
    }

    @Test
    fun `title number is not interpreted as release year`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(
                title = "Blade Runner 2049",
                originalTitle = "Blade Runner 2049",
                year = 2017,
            ),
            listOf(
                indexed(
                    title = "Blade Runner 2049 (2017)",
                    normalizedTitle = "blade runner 2049",
                    year = 2017,
                )
            ),
        )

        assertEquals(TmdbXtreamMatchStatus.MATCHED, result.status)
    }

    @Test
    fun `duplicate source stream is returned only once`() {
        val candidate = indexed()

        val result = TmdbXtreamMatcher.match(
            tmdb(),
            listOf(candidate, candidate),
        )

        assertEquals(1, result.variants.size)
    }


    @Test
    fun `provider subtitle and release metadata produce review candidate`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(
                title = "The Fellowship of the Ring",
                originalTitle = "The Fellowship of the Ring",
                year = 2001,
            ),
            listOf(
                indexed(
                    title = "The Lord of the Rings - The Fellowship of the Ring (2001) [1080p]",
                    normalizedTitle = "the lord of the rings the fellowship of the ring 1080p",
                    year = 2001,
                )
            ),
        )

        assertEquals(TmdbXtreamMatchStatus.NEEDS_REVIEW, result.status)
        assertEquals(1, result.variants.size)
        assertEquals(
            TmdbXtreamMatchConfidence.NEEDS_REVIEW,
            result.variants.first().confidence,
        )
    }

    @Test
    fun `fuzzy matching rejects a one word partial title`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(
                title = "Dune Part Two",
                originalTitle = "Dune Part Two",
                year = 2024,
            ),
            listOf(
                indexed(
                    title = "Dune (2024)",
                    normalizedTitle = "dune",
                    year = 2024,
                )
            ),
        )

        assertEquals(TmdbXtreamMatchStatus.NOT_FOUND, result.status)
    }

    @Test
    fun `one year difference requires manual review`() {
        val result = TmdbXtreamMatcher.match(
            tmdb(
                title = "Dune",
                originalTitle = "Dune",
                year = 2021,
            ),
            listOf(
                indexed(
                    title = "Dune (2020)",
                    normalizedTitle = "dune",
                    year = 2020,
                )
            ),
        )

        assertEquals(TmdbXtreamMatchStatus.NEEDS_REVIEW, result.status)
    }

}
