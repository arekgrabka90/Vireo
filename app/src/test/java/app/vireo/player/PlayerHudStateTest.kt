package app.vireo.player

import app.vireo.domain.model.EpgNowNext
import app.vireo.domain.model.EpgProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHudStateTest {

    @Test
    fun `movie metadata is exposed to HUD`() {
        val item = PlayItem(
            title = "Test movie",
            url = "https://example.invalid/movie.mp4",
            contentType = "MOVIE",
            description = "Movie description",
            year = "2026",
            genre = "Drama",
            duration = "120 min",
            rating = "8.1",
            posterUrl = "poster",
            logoUrl = "movie-logo",
            backdropUrl = "backdrop",
        )

        val result = buildPlayerHudState(
            PlayerUiState(item = item),
            EpgNowNext(null, null),
            emptyList(),
        )

        val movie = result.content as PlayerHudContent.Movie
        assertEquals("Test movie", movie.title)
        assertEquals("Movie description", movie.description)
        assertEquals("2026", movie.year)
        assertEquals("120 min", movie.duration)
        assertEquals("poster", movie.posterUrl)
        assertEquals("movie-logo", movie.logoUrl)
    }

    @Test
    fun `series HUD identifies current episode`() {
        val episode = PlaybackEpisode(
            title = "The episode",
            seasonNumber = 2,
            episodeNumber = 3,
            description = "Episode description",
            thumbnailUrl = "episode-thumb",
        )
        val item = PlayItem(
            title = "Test series",
            url = "https://example.invalid/episode.mp4",
            contentType = "SERIES",
            seriesId = "series-1",
            seasonNumber = 2,
            episodeNumber = 3,
            episodeTitle = "Independent episode title",
            logoUrl = "series-logo",
            episodes = listOf(episode),
        )

        val result = buildPlayerHudState(
            PlayerUiState(item = item),
            EpgNowNext(null, null),
            emptyList(),
        )

        val series = result.content as PlayerHudContent.Series
        assertEquals(episode, series.currentEpisode)
        assertEquals("The episode", series.currentEpisodeTitle)
        assertEquals("series-logo", series.logoUrl)
        assertEquals(listOf(episode), series.episodes)
        assertEquals("Episode description", series.currentEpisodeDescription)
        assertFalse(series.descriptionIsSeriesFallback)
        assertTrue(series.episodes.first().isSelectable)
    }

    @Test
    fun `live HUD exposes EPG and recent channels`() {
        val current = EpgProgram(
            channelId = "channel-1",
            title = "Current programme",
            description = "Programme description",
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
        )
        val recent = RecentLiveChannel(
            sourceId = 1L,
            streamId = "stream-2",
            channelName = "Recent channel",
            epgChannelId = "epg-2",
            logoUrl = "logo",
            lastPlayedAt = 1_500L,
        )
        val item = PlayItem(
            title = "Live channel",
            url = "https://example.invalid/live.m3u8",
            contentType = "LIVE",
            streamId = "stream-1",
            epgChannelId = "epg-1",
            logoUrl = "https://example.invalid/channel-icon.png",
            sourceId = 1L,
        )

        val result = buildPlayerHudState(
            PlayerUiState(item = item),
            EpgNowNext(current, null),
            listOf(recent),
        )

        val live = result.content as PlayerHudContent.Live
        assertEquals(current, live.currentProgram)
        assertEquals("https://example.invalid/channel-icon.png", live.logoUrl)
        assertEquals(1L, live.sourceId)
        assertEquals("Programme description", live.currentProgramDescription)
        assertNull(live.nextProgram)
        assertEquals(listOf(recent), live.recentChannels)
    }

    @Test
    fun `empty playback state produces safe empty content`() {
        val result = buildPlayerHudState(
            PlayerUiState(),
            EpgNowNext(null, null),
            emptyList(),
        )

        assertTrue(result.content is PlayerHudContent.Other)
    }

    @Test
    fun `live HUD uses runtime type fallback and tolerates missing EPG`() {
        val item = PlayItem(
            title = "Live channel",
            url = "https://example.invalid/live.m3u8",
        )

        val result = buildPlayerHudState(
            PlayerUiState(
                item = item,
                runtime = PlayerRuntimeState(contentType = "LIVE"),
            ),
            EpgNowNext(null, null),
            emptyList(),
        )

        val live = result.content as PlayerHudContent.Live
        assertEquals("Live channel", live.channelName)
        assertNull(live.currentProgram)
        assertNull(live.currentProgramDescription)
        assertNull(live.nextProgram)
        assertTrue(live.recentChannels.isEmpty())
    }


    @Test
    fun `series HUD uses PlayItem episode metadata when episode list is empty`() {
        val item = PlayItem(
            title = "Test series",
            url = "https://example.invalid/episode.mp4",
            contentType = "SERIES",
            seriesId = "series-1",
            seasonNumber = 1,
            episodeNumber = 2,
            episodeDescription = "Fallback episode description",
            episodeThumbnailUrl = "fallback-thumbnail",
            episodes = emptyList(),
        )

        val result = buildPlayerHudState(
            PlayerUiState(item = item),
            EpgNowNext(null, null),
            emptyList(),
        )

        val series = result.content as PlayerHudContent.Series
        assertNull(series.currentEpisode)
        assertEquals(
            "Fallback episode description",
            series.currentEpisodeDescription,
        )
        assertEquals(
            "fallback-thumbnail",
            series.currentEpisodeThumbnailUrl,
        )
    }


    @Test
    fun `series overview is an intentional fallback and watched state is exposed`() {
        val episode = PlaybackEpisode(
            title = "Episode without overview",
            seasonNumber = 1,
            episodeNumber = 1,
            description = null,
            thumbnailUrl = "https://example.invalid/still.jpg",
        )
        val item = PlayItem(
            title = "Test series",
            url = "https://example.invalid/episode.mp4",
            contentType = "SERIES",
            contentKey = "tmdb:SERIES:77:S1E1",
            seasonNumber = 1,
            episodeNumber = 1,
            description = "Series overview",
            episodes = listOf(episode),
        )

        val result = buildPlayerHudState(
            state = PlayerUiState(item = item),
            epg = EpgNowNext(null, null),
            recentChannels = emptyList(),
            watchedEpisodeKeys = setOf("tmdb:SERIES:77:S1E1"),
        )

        val series = result.content as PlayerHudContent.Series
        assertEquals("Series overview", series.currentEpisodeDescription)
        assertTrue(series.descriptionIsSeriesFallback)
        assertTrue(series.episodes.first().isWatched)
        assertTrue(series.episodes.first().isSelectable)
    }



    @Test
    fun `movie logo never falls back to poster or backdrop`() {
        val item = PlayItem(
            title = "Movie without title logo",
            url = "https://example.invalid/movie.mp4",
            contentType = "MOVIE",
            posterUrl = "https://example.invalid/poster.jpg",
            backdropUrl = "https://example.invalid/backdrop.jpg",
            logoUrl = null,
        )
        val result = buildPlayerHudState(
            PlayerUiState(item = item),
            EpgNowNext(null, null),
            emptyList(),
        )
        assertNull((result.content as PlayerHudContent.Movie).logoUrl)
    }

    @Test
    fun `series logo never falls back to poster or backdrop`() {
        val item = PlayItem(
            title = "Series without title logo",
            url = "https://example.invalid/episode.mp4",
            contentType = "SERIES",
            seriesId = "series-2",
            posterUrl = "https://example.invalid/poster.jpg",
            backdropUrl = "https://example.invalid/backdrop.jpg",
            logoUrl = null,
        )
        val result = buildPlayerHudState(
            PlayerUiState(item = item),
            EpgNowNext(null, null),
            emptyList(),
        )
        assertNull((result.content as PlayerHudContent.Series).logoUrl)
    }

}
