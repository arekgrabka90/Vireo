package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackFailoverPolicyTest {

    private fun candidate(
        sourceId: Long,
        streamId: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
    ) = PlaybackSourceCandidate(
        sourceId = sourceId,
        streamId = streamId,
        url = url,
        headers = headers,
    )

    @Test
    fun `selects next distinct source`() {
        val current = playbackRequestIdentity("https://one.example/movie.mp4")

        val result = nextFailoverCandidate(
            candidates = listOf(
                candidate(1, "one", "https://one.example/movie.mp4"),
                candidate(2, "two", "https://two.example/movie.mp4"),
            ),
            current = current,
            attempted = emptySet(),
        )

        assertNotNull(result)
        assertEquals("two", result?.streamId)
    }

    @Test
    fun `does not retry an attempted request`() {
        val attempted = playbackRequestIdentity(
            "https://two.example/movie.mp4",
        )!!

        val result = nextFailoverCandidate(
            candidates = listOf(
                candidate(2, "two", "https://two.example/movie.mp4"),
            ),
            current = null,
            attempted = setOf(attempted),
        )

        assertNull(result)
    }

    @Test
    fun `rejects duplicate request identities`() {
        val result = nextFailoverCandidate(
            candidates = listOf(
                candidate(1, "one", "https://same.example/movie.mp4"),
                candidate(2, "two", "https://same.example/movie.mp4"),
            ),
            current = null,
            attempted = emptySet(),
        )

        assertEquals("one", result?.streamId)
    }

    @Test
    fun `different headers produce different request identities`() {
        val first = playbackRequestIdentity(
            "https://media.example/movie.mp4",
            mapOf("Authorization" to "Bearer A"),
        )

        val second = playbackRequestIdentity(
            "https://media.example/movie.mp4",
            mapOf("Authorization" to "Bearer B"),
        )

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(false, first == second)
    }

    @Test
    fun `header names are compared case insensitively`() {
        val first = playbackRequestIdentity(
            "https://media.example/movie.mp4",
            mapOf("Authorization" to "Bearer A"),
        )

        val second = playbackRequestIdentity(
            "https://media.example/movie.mp4",
            mapOf("authorization" to "Bearer A"),
        )

        assertEquals(first, second)
    }

    @Test
    fun `rejects invalid source identifiers`() {
        val result = nextFailoverCandidate(
            candidates = listOf(
                candidate(0, "invalid", "https://one.example/movie.mp4"),
                candidate(1, "", "https://two.example/movie.mp4"),
            ),
            current = null,
            attempted = emptySet(),
        )

        assertNull(result)
    }

    @Test
    fun `rejects blank playback URLs`() {
        val result = nextFailoverCandidate(
            candidates = listOf(
                candidate(1, "one", " "),
            ),
            current = null,
            attempted = emptySet(),
        )

        assertNull(result)
    }

    @Test
    fun `does not expose credentials in identity diagnostics`() {
        val identity = playbackRequestIdentity(
            "https://media.example/movie.mp4?token=secret",
            mapOf("Authorization" to "Bearer secret"),
        )

        assertNotNull(identity)
        assertEquals(
            "PlaybackRequestIdentity(redacted)",
            identity.toString(),
        )
    }
}
