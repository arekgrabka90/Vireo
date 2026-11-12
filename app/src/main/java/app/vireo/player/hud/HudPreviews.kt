package app.vireo.player.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.vireo.domain.model.EpgProgram
import app.vireo.player.PlaybackEpisode
import app.vireo.player.PlayerHudContent
import app.vireo.player.PlayerHudOverlay
import app.vireo.player.PlayerHudState
import app.vireo.player.PlayerProgressState
import app.vireo.player.PlayerTransportState
import app.vireo.player.RecentLiveChannel

/** Stands in for the real video so the transparency of the HUD is visible in previews. */
@Composable
private fun PreviewVideo(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF4A5C70), Color(0xFF10151B)))),
    ) { content() }
}

private fun state(content: PlayerHudContent, playing: Boolean = true) = PlayerHudState(
    content = content,
    transportState = PlayerTransportState.READY,
    isPlaying = playing,
    hasPrevious = false,
    hasNext = false,
    controlsVisible = true,
    errorMessage = null,
)

@Composable
private fun PreviewHud(
    hud: PlayerHudState,
    progress: PlayerProgressState = PlayerProgressState(),
    withActions: Boolean = false,
) {
    PreviewVideo {
        PlayerHudOverlay(
            hudState = hud,
            progress = progress,
            onBack = {}, onPlayPause = {}, onSeekBackward = {}, onSeekForward = {},
            onSeekTo = {}, onPrevious = {}, onNext = {},
            onSelectEpisode = { _, _ -> }, onCancelNextEpisodeCountdown = {},
            onVisibilityChanged = {},
            onSelectRecentChannel = if (withActions) ({}) else null,
            onOpenGuide = if (withActions) ({}) else null,
        )
    }
}

private fun liveContent(
    withProgram: Boolean,
    withLogo: Boolean,
    withNext: Boolean = true,
): PlayerHudContent.Live {
    val now = System.currentTimeMillis()
    val current = EpgProgram(
        channelId = "c1",
        title = "Sample programme",
        description = "A short sample description used only in previews. It is long enough to show the three-line limit and the ellipsis behaviour of the HUD description block.",
        startTimeMs = now - 20 * 60_000L,
        endTimeMs = now + 40 * 60_000L,
    )
    return PlayerHudContent.Live(
        channelName = "Sample Channel",
        epgChannelId = "c1",
        streamId = "100",
        // Network is unavailable in previews, so a logo URL renders the text fallback here.
        logoUrl = if (withLogo) "https://example.invalid/logo.png" else null,
        currentProgram = if (withProgram) current else null,
        currentProgramDescription = if (withProgram) current.description else null,
        nextProgram = if (withProgram && withNext) {
            current.copy(title = "Next sample programme", startTimeMs = current.endTimeMs, endTimeMs = current.endTimeMs + 60 * 60_000L)
        } else null,
        recentChannels = listOf(
            RecentLiveChannel(1, "201", "News One", null, null, 0L),
            RecentLiveChannel(1, "202", "Sport Two", null, null, 0L),
            RecentLiveChannel(1, "203", "Movies Three", null, null, 0L),
        ),
        sourceId = 1L,
    )
}

@Preview(name = "Live – full data + tiles", widthDp = 1280, heightDp = 720)
@Composable
private fun LiveFullPreview() = PreviewHud(
    hud = state(liveContent(withProgram = true, withLogo = true)),
    withActions = true,
)

@Preview(name = "Live – no logo, no EPG", widthDp = 1280, heightDp = 720)
@Composable
private fun LiveEmptyPreview() = PreviewHud(
    hud = state(liveContent(withProgram = false, withLogo = false)),
)

@Preview(name = "Movie – no logo (title text)", widthDp = 1280, heightDp = 720)
@Composable
private fun MoviePreview() = PreviewHud(
    hud = state(
        PlayerHudContent.Movie(
            title = "Sample Movie", description = "Short movie description for the preview.",
            year = "2026", genre = "Drama", duration = "2 h 04 min", rating = "8.1",
            posterUrl = null, backdropUrl = null, logoUrl = null,
        ),
        playing = false, // paused: the video behind must stay visible
    ),
    progress = PlayerProgressState(positionMs = 1_800_000L, durationMs = 7_440_000L, isSeekable = true),
)

@Preview(name = "Movie – only a title", widthDp = 1280, heightDp = 720)
@Composable
private fun MovieMinimalPreview() = PreviewHud(
    hud = state(
        PlayerHudContent.Movie(
            title = "Sample Movie", description = null, year = null, genre = null,
            duration = null, rating = null, posterUrl = null, backdropUrl = null, logoUrl = null,
        ),
    ),
)

@Preview(name = "Series – episode", widthDp = 1280, heightDp = 720)
@Composable
private fun SeriesPreview() {
    val episode = PlaybackEpisode(
        title = "Sample episode", seasonNumber = 1, episodeNumber = 2,
        description = "Episode description for the preview.", duration = "48 min",
    )
    PreviewHud(
        hud = state(
            PlayerHudContent.Series(
                title = "Sample Series", description = "Series description.", year = "2025",
                genre = "Thriller", rating = "7.9", posterUrl = null, backdropUrl = null, logoUrl = null,
                currentEpisode = episode, currentEpisodeTitle = episode.title,
                currentSeasonNumber = episode.seasonNumber, currentEpisodeNumber = episode.episodeNumber,
                currentEpisodeDescription = episode.description,
                currentEpisodeThumbnailUrl = null, episodes = listOf(episode),
                descriptionIsSeriesFallback = false,
            ),
        ),
        progress = PlayerProgressState(positionMs = 600_000L, durationMs = 2_880_000L, isSeekable = true),
    )
}

@Preview(name = "Compact (phone)", widthDp = 411, heightDp = 891)
@Composable
private fun CompactPreview() = PreviewHud(hud = state(liveContent(withProgram = true, withLogo = false)))

@Preview(name = "Compact landscape phone", widthDp = 891, heightDp = 411)
@Composable
private fun CompactLandscapePreview() = PreviewHud(
    hud = state(liveContent(withProgram = true, withLogo = false)),
)
