package app.vireo.player

internal data class EpisodeFailoverCandidate(
    val sourceId: Long,
    val streamId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val url: String,
)

/**
 * Only an exact season + episode match may be used as an alternative.
 * This policy deliberately does not infer matches from titles or list order.
 */
internal fun exactEpisodeFailoverCandidates(
    seasonNumber: Int,
    episodeNumber: Int,
    candidates: List<EpisodeFailoverCandidate>,
): List<PlaybackSourceCandidate> =
    candidates.asSequence()
        .filter {
            it.seasonNumber == seasonNumber &&
                it.episodeNumber == episodeNumber &&
                it.sourceId > 0L &&
                it.streamId.isNotBlank() &&
                it.url.isNotBlank()
        }
        .map {
            PlaybackSourceCandidate(
                sourceId = it.sourceId,
                streamId = it.streamId,
                url = it.url,
            )
        }
        .toList()
