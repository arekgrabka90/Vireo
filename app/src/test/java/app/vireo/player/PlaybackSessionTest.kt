package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSessionTest {

    @Test
    fun `live EPG channel id survives playback session`() {
        val session = PlaybackSession()
        val item = PlayItem(
            title = "Test channel",
            url = "https://example.invalid/live.m3u8",
            contentType = "LIVE",
            epgChannelId = "test.channel",
        )

        session.start(listOf(item), 0)

        assertEquals("test.channel", session.current()?.epgChannelId)
        assertEquals("LIVE", session.current()?.contentType)
        assertNull(session.current()?.contentKey)
    }

    @Test
    fun `play item retains metadata and episode list`() {
        val session = PlaybackSession()
        val item = PlayItem(
            title = "Example series",
            url = "https://example.invalid/episode.m3u8",
            contentType = "SERIES",
            description = "Series overview",
            year = "2025",
            genre = "Drama",
            episodeDescription = "Episode overview",
            episodeTitle = "Episode title",
            logoUrl = "series-logo",
            episodeThumbnailUrl = "https://example.invalid/still.jpg",
            episodes = listOf(
                PlaybackEpisode(
                    title = "Episode 1",
                    seasonNumber = 1,
                    episodeNumber = 1,
                    description = "First episode",
                    duration = "45 min",
                    thumbnailUrl = "https://example.invalid/one.jpg",
                )
            ),
        )

        session.start(listOf(item), 0)

        assertEquals("Series overview", session.current()?.description)
        assertEquals("2025", session.current()?.year)
        assertEquals("Episode overview", session.current()?.episodeDescription)
        assertEquals("Episode title", session.current()?.episodeTitle)
        assertEquals("series-logo", session.current()?.logoUrl)
        assertEquals(1, session.current()?.episodes?.size)
        assertEquals("Episode 1", session.current()?.episodes?.first()?.title)
        assertEquals("First episode", session.current()?.episodes?.first()?.description)
        assertEquals("45 min", session.current()?.episodes?.first()?.duration)
        assertEquals(
            "https://example.invalid/one.jpg",
            session.current()?.episodes?.first()?.thumbnailUrl,
        )
    }

    @Test
    fun `next item resets alternative selection`() {
        val session = PlaybackSession()
        val first = PlayItem(
            title = "First",
            url = "https://example.invalid/first.m3u8",
            alternatives = listOf(
                PlaybackAlternative(
                    sourceId = 1L,
                    streamId = "alt-1",
                    url = "https://example.invalid/first-alt.m3u8",
                )
            ),
        )
        val second = PlayItem(
            title = "Second",
            url = "https://example.invalid/second.m3u8",
        )

        session.start(listOf(first, second), 0)
        session.nextAlternative()
        session.next()

        assertEquals("Second", session.current()?.title)
        assertEquals(
            "https://example.invalid/second.m3u8",
            session.current()?.url,
        )
    }

    @Test
    fun `select episode moves queue and resets alternative source`() {
        val session = PlaybackSession()
        val first = PlayItem(
            title = "Test series",
            url = "https://example.invalid/s1e1.mp4",
            contentType = "SERIES",
            seasonNumber = 1,
            episodeNumber = 1,
            alternatives = listOf(
                PlaybackAlternative(
                    sourceId = 2L,
                    streamId = "alternative",
                    url = "https://example.invalid/alternative.mp4",
                )
            ),
        )
        val second = PlayItem(
            title = "Test series",
            url = "https://example.invalid/s1e2.mp4",
            contentType = "SERIES",
            seasonNumber = 1,
            episodeNumber = 2,
        )

        session.start(listOf(first, second), 0)
        session.nextAlternative()

        val selected = session.selectEpisode(1, 2)

        assertEquals(2, selected?.episodeNumber)
        assertEquals(
            "https://example.invalid/s1e2.mp4",
            session.current()?.url,
        )
        assertNull(session.selectEpisode(9, 9))
    }

}
