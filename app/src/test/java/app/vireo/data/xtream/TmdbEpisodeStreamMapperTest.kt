package app.vireo.data.xtream

import app.vireo.data.tmdb.TmdbEpisodeItem
import app.vireo.domain.model.Episode
import app.vireo.domain.model.Season
import app.vireo.domain.model.SeriesDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbEpisodeStreamMapperTest {

    private fun tmdbEpisode(number: Int, title: String = "TMDB episode") =
        TmdbEpisodeItem(
            tmdbEpisodeId = number + 100,
            seasonNumber = 1,
            episodeNumber = number,
            title = title,
            overview = "TMDB description",
            stillUrl = "https://image.tmdb.org/t/p/w780/$number.jpg",
            runtimeMinutes = 42,
            airDate = null,
        )

    private fun streamEpisode(
        id: String,
        number: Int,
        url: String,
    ) = Episode(
        id = id,
        number = number,
        title = "Xtream title must not be used",
        plot = "Xtream description must not be used",
        duration = "99 min",
        thumbUrl = "https://example.com/xtream.jpg",
        streamUrl = url,
    )

    private fun series(vararg episodes: Episode) = SeriesDetails(
        id = "series-1",
        name = "Xtream series title",
        posterUrl = null,
        backdropUrl = null,
        plot = null,
        genre = null,
        year = null,
        cast = null,
        rating = null,
        seasons = listOf(Season(1, episodes.toList())),
    )

    @Test
    fun `TMDB metadata is paired with exact Xtream stream locator`() {
        val result = mapTmdbEpisodeStreams(
            listOf(tmdbEpisode(1, "Canonical TMDB title")),
            series(streamEpisode("stream-1", 1, "https://stream.example/1")),
        )

        assertEquals(1, result.size)
        assertEquals("Canonical TMDB title", result.single().tmdbEpisode.title)
        assertEquals("https://stream.example/1", result.single().streamEpisode.streamUrl)
    }

    @Test
    fun `ambiguous Xtream episode matches are skipped`() {
        val result = mapTmdbEpisodeStreams(
            listOf(tmdbEpisode(1)),
            series(
                streamEpisode("stream-1", 1, "https://stream.example/1"),
                streamEpisode("stream-2", 1, "https://stream.example/2"),
            ),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `episodes without a playable stream are skipped`() {
        val result = mapTmdbEpisodeStreams(
            listOf(tmdbEpisode(1)),
            series(streamEpisode("stream-1", 1, " ")),
        )

        assertTrue(result.isEmpty())
    }
}
