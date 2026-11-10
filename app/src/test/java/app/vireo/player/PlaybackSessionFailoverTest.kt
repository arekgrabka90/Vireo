package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionFailoverTest {

    @Test
    fun `next alternative changes only current source`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(
                    title = "Movie",
                    url = "https://primary.example/movie.mp4",
                    contentKey = "tmdb:MOVIE:123",
                    sourceId = 1L,
                    streamId = "primary-1",
                    alternatives = listOf(
                        PlaybackAlternative(
                            sourceId = 2L,
                            streamId = "alt-2",
                            url = "https://secondary.example/movie.mp4",
                        ),
                    ),
                ),
            ),
            0,
        )

        val next = session.nextAlternative()

        assertEquals(
            "https://secondary.example/movie.mp4",
            next?.url,
        )
        assertEquals("tmdb:MOVIE:123", next?.contentKey)
        assertEquals("Movie", next?.title)
        assertEquals(2L, next?.sourceId)
        assertEquals("alt-2", next?.streamId)
    }

    @Test
    fun `alternative list is exhausted safely`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(
                    title = "Movie",
                    url = "https://primary.example/movie.mp4",
                ),
            ),
            0,
        )

        assertNull(session.nextAlternative())
    }

    @Test
    fun `next episode resets alternative selection`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(
                    title = "Episode 1",
                    url = "https://one.example/1.mp4",
                    alternatives = listOf(
                        PlaybackAlternative(
                            sourceId = 2L,
                            streamId = "alt-1",
                            url = "https://two.example/1.mp4",
                        ),
                    ),
                ),
                PlayItem(
                    title = "Episode 2",
                    url = "https://one.example/2.mp4",
                ),
            ),
            0,
        )

        assertEquals(
            "https://two.example/1.mp4",
            session.nextAlternative()?.url,
        )

        assertEquals(
            "https://one.example/2.mp4",
            session.next()?.url,
        )
    }

    @Test
    fun `primary source remains first`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(
                    title = "Movie",
                    url = "https://primary.example/movie.mp4",
                    alternatives = listOf(
                        PlaybackAlternative(
                            sourceId = 2L,
                            streamId = "alt",
                            url = "https://secondary.example/movie.mp4",
                        ),
                    ),
                ),
            ),
            0,
        )

        assertTrue(
            session.current()?.url == "https://primary.example/movie.mp4",
        )
    }
}
