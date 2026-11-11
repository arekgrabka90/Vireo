package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EpisodeFailoverSeriesIdTest {

    private fun entry(streamId: String, seriesId: String) =
        EpisodeFailoverSeriesCandidate(
            candidate = EpisodeFailoverCandidate(
                sourceId = 7L,
                streamId = streamId,
                seasonNumber = 1,
                episodeNumber = 2,
                url = "https://example.test/$streamId",
            ),
            seriesId = seriesId,
        )

    @Test
    fun mapsCandidatesToTheirExactParentSeries() {
        val result = exactEpisodeSeriesIds(
            listOf(entry("episode-a", "series-a"), entry("episode-b", "series-b"))
        )

        assertEquals("series-a", result[Triple(7L, "episode-a", "https://example.test/episode-a")])
        assertEquals("series-b", result[Triple(7L, "episode-b", "https://example.test/episode-b")])
    }

    @Test
    fun rejectsAmbiguousParentSeriesForSameCandidate() {
        val result = exactEpisodeSeriesIds(
            listOf(entry("episode-a", "series-a"), entry("episode-a", "series-b"))
        )

        assertFalse(result.containsKey(Triple(7L, "episode-a", "https://example.test/episode-a")))
    }

    @Test
    fun allowsDuplicateCandidateWhenParentSeriesIsIdentical() {
        val result = exactEpisodeSeriesIds(
            listOf(entry("episode-a", "series-a"), entry("episode-a", "series-a"))
        )

        assertEquals("series-a", result[Triple(7L, "episode-a", "https://example.test/episode-a")])
    }
}
