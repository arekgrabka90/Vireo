package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbDetailMapperTest {

    private fun detail() = TmdbDetailDto(
        id = 100,
        title = "Example",
    )

    @Test
    fun `Polish logo wins before higher rated English logo`() {
        val result = TmdbDetailMapper.map(
            detail(),
            ContentType.MOVIE,
            TmdbImagesDto(
                logos = listOf(
                    TmdbLogoDto(
                        filePath = "/english.png",
                        iso6391 = "en",
                        voteAverage = 10.0,
                        width = 3000,
                    ),
                    TmdbLogoDto(
                        filePath = "/polish.png",
                        iso6391 = "pl",
                        voteAverage = 5.0,
                        width = 1000,
                    ),
                )
            ),
        )

        assertEquals(
            "https://image.tmdb.org/t/p/w500/polish.png",
            result?.logoUrl,
        )
    }

    @Test
    fun `English logo is preferred when Polish is unavailable`() {
        val result = TmdbDetailMapper.map(
            detail(),
            ContentType.MOVIE,
            TmdbImagesDto(
                logos = listOf(
                    TmdbLogoDto(filePath = "/untagged.png", iso6391 = null, voteAverage = 10.0),
                    TmdbLogoDto(filePath = "/other.png", iso6391 = "de", voteAverage = 9.0),
                    TmdbLogoDto(filePath = "/english.png", iso6391 = "en", voteAverage = 5.0),
                )
            ),
        )

        assertEquals(
            "https://image.tmdb.org/t/p/w500/english.png",
            result?.logoUrl,
        )
    }

    @Test
    fun `logo selection falls back to first TMDB logo`() {
        val result = TmdbDetailMapper.map(
            detail(),
            ContentType.MOVIE,
            TmdbImagesDto(
                logos = listOf(
                    TmdbLogoDto(filePath = "/fallback.svg", iso6391 = null),
                    TmdbLogoDto(filePath = "/other.svg", iso6391 = "en"),
                )
            ),
        )

        assertEquals(
            "https://image.tmdb.org/t/p/w500/fallback.svg",
            result?.logoUrl,
        )
    }

    @Test
    fun `official YouTube trailer is preferred over teaser`() {
        val result = TmdbDetailMapper.map(
            detail(),
            ContentType.MOVIE,
            videos = listOf(
                TmdbVideoDto(
                    key = "teaser-key",
                    site = "YouTube",
                    type = "Teaser",
                ),
                TmdbVideoDto(
                    key = "official-trailer",
                    site = "YouTube",
                    type = "Trailer",
                    official = true,
                    size = 1080,
                ),
                TmdbVideoDto(
                    key = "non-youtube",
                    site = "Vimeo",
                    type = "Trailer",
                    official = true,
                ),
            ),
        )

        assertEquals("official-trailer", result?.trailerYoutubeKey)
    }

    @Test
    fun `trailer remains empty when no YouTube video exists`() {
        val result = TmdbDetailMapper.map(
            detail(),
            ContentType.MOVIE,
            videos = listOf(
                TmdbVideoDto(
                    key = "vimeo-key",
                    site = "Vimeo",
                    type = "Trailer",
                ),
            ),
        )

        assertNull(result?.trailerYoutubeKey)
    }
    @Test
    fun `missing TMDB episode name stays empty instead of being fabricated`() {
        val result = TmdbDetailMapper.mapEpisode(
            TmdbEpisodeDto(
                id = 10,
                seasonNumber = 1,
                episodeNumber = 2,
            )
        )

        assertEquals("", result?.title)
    }
}
