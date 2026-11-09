package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeFailoverPolicyTest {

    @Test
    fun `accepts only exact season and episode`() {
        val result = exactEpisodeFailoverCandidates(
            seasonNumber = 2,
            episodeNumber = 5,
            candidates = listOf(
                EpisodeFailoverCandidate(
                    2L, "s2e5", 2, 5, "https://two.example/s02e05.mp4",
                ),
                EpisodeFailoverCandidate(
                    3L, "s2e6", 2, 6, "https://three.example/s02e06.mp4",
                ),
                EpisodeFailoverCandidate(
                    4L, "s1e5", 1, 5, "https://four.example/s01e05.mp4",
                ),
            ),
        )

        assertEquals(listOf("s2e5"), result.map { it.streamId })
    }

    @Test
    fun `rejects invalid identifiers and blank urls`() {
        val result = exactEpisodeFailoverCandidates(
            seasonNumber = 1,
            episodeNumber = 1,
            candidates = listOf(
                EpisodeFailoverCandidate(
                    0L, "bad-source", 1, 1, "https://x.example/1.mp4",
                ),
                EpisodeFailoverCandidate(
                    2L, "", 1, 1, "https://x.example/2.mp4",
                ),
                EpisodeFailoverCandidate(
                    3L, "blank-url", 1, 1, " ",
                ),
            ),
        )

        assertEquals(emptyList<PlaybackSourceCandidate>(), result)
    }
}
