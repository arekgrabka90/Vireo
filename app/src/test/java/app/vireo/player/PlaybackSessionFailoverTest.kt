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

    @Test
    fun `series alternative preserves exact episode source identity`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(
                    title = "Series · S2 E4",
                    url = "https://primary.example/s2e4.mp4",
                    contentType = "SERIES",
                    sourceId = 1L,
                    streamId = "primary-episode",
                    seriesId = "primary-series",
                    seasonNumber = 2,
                    episodeNumber = 4,
                    alternatives = listOf(
                        PlaybackAlternative(
                            sourceId = 2L,
                            streamId = "alternative-episode",
                            url = "https://alternative.example/s2e4.mp4",
                            seriesId = "alternative-series",
                            seasonNumber = 2,
                            episodeNumber = 4,
                        ),
                    ),
                ),
            ),
            0,
        )

        val alternative = session.nextAlternative()

        assertEquals("https://alternative.example/s2e4.mp4", alternative?.url)
        assertEquals(2L, alternative?.sourceId)
        assertEquals("alternative-episode", alternative?.streamId)
        assertEquals("alternative-series", alternative?.seriesId)
        assertEquals(2, alternative?.seasonNumber)
        assertEquals(4, alternative?.episodeNumber)
    }

    @Test
    fun `series queue keeps earlier episodes and starts at selected episode`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(title = "E01", url = "https://example.invalid/e01.mp4"),
                PlayItem(title = "E02", url = "https://example.invalid/e02.mp4"),
                PlayItem(title = "E03", url = "https://example.invalid/e03.mp4"),
            ),
            1,
        )

        assertEquals(3, session.items.size)
        assertEquals(1, session.index)
        assertEquals("E02", session.current()?.title)
        assertEquals("E03", session.next()?.title)
        assertEquals("E01", session.items.first().title)
    }


    @Test
    fun `previous episode resets alternative selection`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(
                    title = "Episode 1",
                    url = "https://example.invalid/1.mp4",
                ),
                PlayItem(
                    title = "Episode 2",
                    url = "https://example.invalid/2.mp4",
                    alternatives = listOf(
                        PlaybackAlternative(
                            sourceId = 2L,
                            streamId = "alternative",
                            url = "https://alternative.invalid/2.mp4",
                        ),
                    ),
                ),
            ),
            1,
        )

        assertEquals(
            "https://alternative.invalid/2.mp4",
            session.nextAlternative()?.url,
        )

        assertEquals("Episode 1", session.previous()?.title)
        assertEquals("https://example.invalid/1.mp4", session.current()?.url)
        assertTrue(!session.hasPrevious())
        assertTrue(session.hasNext())
    }

    @Test
    fun `previous and next availability follows queue index`() {
        val session = PlaybackSession()
        session.start(
            listOf(
                PlayItem(title = "E01", url = "https://example.invalid/1"),
                PlayItem(title = "E02", url = "https://example.invalid/2"),
                PlayItem(title = "E03", url = "https://example.invalid/3"),
            ),
            1,
        )

        assertTrue(session.hasPrevious())
        assertTrue(session.hasNext())

        assertEquals("E01", session.previous()?.title)
        assertTrue(!session.hasPrevious())
        assertTrue(session.hasNext())

        assertEquals("E02", session.next()?.title)
        assertTrue(session.hasPrevious())
        assertTrue(session.hasNext())
    }

    @Test
    fun `empty queue has no previous or next item`() {
        val session = PlaybackSession()
        session.start(emptyList(), 0)

        assertNull(session.previous())
        assertTrue(!session.hasPrevious())
        assertTrue(!session.hasNext())
    }

}
