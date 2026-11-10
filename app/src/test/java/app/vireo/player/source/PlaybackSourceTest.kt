package app.vireo.player.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSourceTest {

    @Test
    fun `normalizes HTTP headers`() {
        val source = PlaybackSource(
            url = "https://media.example/movie.m3u8",
            headers = mapOf(
                " Authorization " to " Bearer secret ",
                "User-Agent" to "Vireo",
            ),
        )

        assertEquals("Bearer secret", source.headers["authorization"])
        assertEquals("Vireo", source.headers["user-agent"])
    }

    @Test
    fun `valid source converts to existing failover candidate`() {
        val source = PlaybackSource(
            url = "https://media.example/movie.m3u8",
            sourceId = 7L,
            streamId = "movie-7",
            headers = mapOf("Authorization" to "Bearer token"),
            role = PlaybackSourceRole.ALTERNATIVE,
        )

        val candidate = source.toCandidate()

        assertNotNull(candidate)
        assertEquals(7L, candidate?.sourceId)
        assertEquals("movie-7", candidate?.streamId)
        assertEquals("https://media.example/movie.m3u8", candidate?.url)
        assertEquals("Bearer token", candidate?.headers?.get("authorization"))
    }

    @Test
    fun `source without valid identifiers cannot become candidate`() {
        val missingId = PlaybackSource(
            url = "https://media.example/movie.m3u8",
            sourceId = null,
            streamId = "movie",
        )

        val invalidId = PlaybackSource(
            url = "https://media.example/movie.m3u8",
            sourceId = 0L,
            streamId = "movie",
        )

        val blankStream = PlaybackSource(
            url = "https://media.example/movie.m3u8",
            sourceId = 1L,
            streamId = " ",
        )

        assertNull(missingId.toCandidate())
        assertNull(invalidId.toCandidate())
        assertNull(blankStream.toCandidate())
    }

    @Test
    fun `diagnostics do not expose URL or authorization credentials`() {
        val source = PlaybackSource(
            url = "https://media.example/movie.m3u8?token=secret",
            sourceId = 1L,
            streamId = "movie",
            headers = mapOf("Authorization" to "Bearer secret"),
        )

        val diagnostic = source.toString()

        assertTrue(diagnostic.contains("redacted"))
        assertTrue(!diagnostic.contains("secret"))
        assertTrue(!diagnostic.contains("https://"))
    }

    @Test
    fun `invalid URL cannot become a request candidate`() {
        val source = PlaybackSource(
            url = " ",
            sourceId = 1L,
            streamId = "movie",
        )

        assertNull(source.toCandidate())
    }
}
