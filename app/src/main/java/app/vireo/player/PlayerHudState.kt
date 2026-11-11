package app.vireo.player

import app.vireo.domain.model.EpgNowNext

/**
 * UI-ready data for the future shared Compose HUD.
 * This model has no Compose or Media3 dependencies.
 */
data class PlayerHudState(
    val content: PlayerHudContent,
    val transportState: PlayerTransportState,
    val isPlaying: Boolean,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val controlsVisible: Boolean,
    val errorMessage: String?,
)

sealed interface PlayerHudContent {
    data class Movie(
        val title: String,
        val description: String?,
        val year: String?,
        val genre: String?,
        val duration: String?,
        val rating: String?,
        val posterUrl: String?,
        val logoUrl: String?,
        val backdropUrl: String?,
    ) : PlayerHudContent

    data class Series(
        val title: String,
        val description: String?,
        val year: String?,
        val genre: String?,
        val rating: String?,
        val posterUrl: String?,
        val logoUrl: String?,
        val backdropUrl: String?,
        val currentEpisode: PlaybackEpisode?,
        val currentEpisodeTitle: String?,
        val currentSeasonNumber: Int?,
        val currentEpisodeNumber: Int?,
        val currentEpisodeDescription: String?,
        val currentEpisodeThumbnailUrl: String?,
        val episodes: List<PlaybackEpisode>,
    ) : PlayerHudContent

    data class Live(
        val channelName: String,
        val epgChannelId: String?,
        val streamId: String?,
        val logoUrl: String?,
        val currentProgram: app.vireo.domain.model.EpgProgram?,
        val currentProgramDescription: String?,
        val nextProgram: app.vireo.domain.model.EpgProgram?,
        val recentChannels: List<RecentLiveChannel>,
    ) : PlayerHudContent

    data class Other(
        val title: String,
        val posterUrl: String?,
    ) : PlayerHudContent
}

data class RecentLiveChannel(
    val sourceId: Long,
    val streamId: String,
    val channelName: String,
    val epgChannelId: String?,
    val logoUrl: String?,
    val lastPlayedAt: Long,
)

internal fun buildPlayerHudState(
    state: PlayerUiState,
    epg: EpgNowNext,
    recentChannels: List<RecentLiveChannel>,
): PlayerHudState {
    val item = state.item

    val contentType = item?.contentType ?: state.runtime.contentType

    val content = when {
        item == null -> PlayerHudContent.Other("", null)

        contentType == "LIVE" -> PlayerHudContent.Live(
            channelName = item.title,
            epgChannelId = item.epgChannelId,
            streamId = item.streamId,
            logoUrl = item.logoUrl,
            currentProgram = epg.current,
            currentProgramDescription = epg.current?.description,
            nextProgram = epg.next,
            recentChannels = recentChannels,
        )

        contentType == "SERIES" ||
            item.seriesId != null ||
            item.episodes.isNotEmpty() -> {
            val currentEpisode = item.episodes.firstOrNull {
                it.seasonNumber == item.seasonNumber &&
                    it.episodeNumber == item.episodeNumber
            }

            PlayerHudContent.Series(
                title = item.title,
                description = item.description,
                year = item.year,
                genre = item.genre,
                rating = item.rating,
                posterUrl = item.posterUrl,
                logoUrl = item.logoUrl,
                backdropUrl = item.backdropUrl,
                currentEpisode = currentEpisode,
                currentEpisodeTitle = currentEpisode?.title ?: item.episodeTitle,
                currentSeasonNumber = currentEpisode?.seasonNumber ?: item.seasonNumber,
                currentEpisodeNumber = currentEpisode?.episodeNumber ?: item.episodeNumber,
                currentEpisodeDescription =
                    currentEpisode?.description ?: item.episodeDescription,
                currentEpisodeThumbnailUrl =
                    currentEpisode?.thumbnailUrl ?: item.episodeThumbnailUrl,
                episodes = item.episodes,
            )
        }

        contentType == "MOVIE" -> PlayerHudContent.Movie(
            title = item.title,
            description = item.description,
            year = item.year,
            genre = item.genre,
            duration = item.duration,
            rating = item.rating,
            posterUrl = item.posterUrl,
            logoUrl = item.logoUrl,
            backdropUrl = item.backdropUrl,
        )

        else -> PlayerHudContent.Other(
            title = item.title,
            posterUrl = item.posterUrl,
        )
    }

    return PlayerHudState(
        content = content,
        transportState = state.runtime.transportState,
        isPlaying = state.runtime.isPlaying,
        hasPrevious = state.hasPrevious,
        hasNext = state.hasNext,
        controlsVisible = state.runtime.controlsVisible,
        errorMessage = state.error,
    )
}
