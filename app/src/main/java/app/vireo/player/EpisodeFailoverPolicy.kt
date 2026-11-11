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

internal data class EpisodeFailoverSeriesCandidate(
    val candidate: EpisodeFailoverCandidate,
    val seriesId: String,
)

internal fun exactEpisodeSeriesIds(
    candidates: List<EpisodeFailoverSeriesCandidate>,
): Map<Triple<Long, String, String>, String> =
    candidates
        .groupBy {
            Triple(it.candidate.sourceId, it.candidate.streamId, it.candidate.url)
        }
        .mapNotNull { (key, entries) ->
            val seriesIds = entries.map { it.seriesId }.distinct()
            if (seriesIds.size == 1 && seriesIds.single().isNotBlank()) {
                key to seriesIds.single()
            } else {
                null
            }
        }
        .toMap()
