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
    val seriesNextEpisodeCountdownSeconds: Int? = null,
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
        /** Rzeczywiste logo tytulu z TMDB; nigdy poster ani backdrop. */
        /** Rzeczywiste logo serialu z TMDB; nigdy poster ani backdrop. */
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
        val descriptionIsSeriesFallback: Boolean = false,
    ) : PlayerHudContent

    data class Live(
        val channelName: String,
        val epgChannelId: String?,
        val streamId: String?,
        /** Ikona kanalu Xtream (stream_icon), nie logo tytulu TMDB. */
        val logoUrl: String?,
        val currentProgram: app.vireo.domain.model.EpgProgram?,
        val currentProgramDescription: String?,
        val nextProgram: app.vireo.domain.model.EpgProgram?,
        val recentChannels: List<RecentLiveChannel>,
        /** Konto Xtream, z ktorego pochodzi kanal; odroznia historie miedzy zrodlami. */
        val sourceId: Long? = null,
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
    watchedEpisodeKeys: Set<String> = emptySet(),
    playbackQueue: List<PlayItem> = listOfNotNull(state.item),
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
            sourceId = item.sourceId,
        )

        contentType == "SERIES" ||
            item.seriesId != null ||
            item.episodes.isNotEmpty() -> {
            val contentKeyPrefix = item.contentKey
                ?.takeIf { ":S" in it }
                ?.substringBeforeLast(":S")

            val queuedEpisodeNumbers = playbackQueue
                .asSequence()
                .filter { it.contentType == "SERIES" }
                .mapNotNull { queued ->
                    val season = queued.seasonNumber ?: return@mapNotNull null
                    val episode = queued.episodeNumber ?: return@mapNotNull null
                    season to episode
                }
                .toSet()

            val episodes = item.episodes.map { episode ->
                val episodeKey = contentKeyPrefix?.let {
                    "$it:S${episode.seasonNumber}E${episode.episodeNumber}"
                }
                episode.copy(
                    isWatched = episodeKey != null &&
                        episodeKey in watchedEpisodeKeys,
                    isSelectable =
                        (episode.seasonNumber to episode.episodeNumber) in
                            queuedEpisodeNumbers,
                )
            }

            val currentEpisode = episodes.firstOrNull {
                it.seasonNumber == item.seasonNumber &&
                    it.episodeNumber == item.episodeNumber
            }

            val episodeDescription = currentEpisode?.description
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: item.episodeDescription
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            val descriptionIsSeriesFallback =
                episodeDescription == null &&
                    !item.description.isNullOrBlank()

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
                currentEpisodeDescription = episodeDescription
                    ?: item.description?.trim()?.takeIf { it.isNotBlank() },
                currentEpisodeThumbnailUrl =
                    currentEpisode?.thumbnailUrl ?: item.episodeThumbnailUrl,
                episodes = episodes,
                descriptionIsSeriesFallback = descriptionIsSeriesFallback,
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
        seriesNextEpisodeCountdownSeconds =
            state.seriesNextEpisodeCountdownSeconds,
    )
}
