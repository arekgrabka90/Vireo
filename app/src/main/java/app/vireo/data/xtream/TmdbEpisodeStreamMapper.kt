package app.vireo.data.xtream

import app.vireo.data.tmdb.TmdbEpisodeItem
import app.vireo.domain.model.Episode
import app.vireo.domain.model.SeriesDetails

internal data class TmdbEpisodeStreamMatch(
    val tmdbEpisode: TmdbEpisodeItem,
    val streamEpisode: Episode,
)

internal fun mapTmdbEpisodeStreams(
    tmdbEpisodes: List<TmdbEpisodeItem>,
    xtreamSeries: SeriesDetails,
): List<TmdbEpisodeStreamMatch> {
    val tmdbByKey = tmdbEpisodes.groupBy {
        it.seasonNumber to it.episodeNumber
    }

    val xtreamByKey = xtreamSeries.seasons
        .flatMap { season ->
            season.episodes.map { episode ->
                (season.number to episode.number) to episode
            }
        }
        .groupBy({ it.first }, { it.second })

    return (tmdbByKey.keys intersect xtreamByKey.keys)
        .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
        .mapNotNull { key ->
            val tmdbEpisode = tmdbByKey[key]?.singleOrNull()
                ?: return@mapNotNull null
            val streamEpisode = xtreamByKey[key]?.singleOrNull()
                ?.takeIf {
                    it.id.isNotBlank() && it.streamUrl.isNotBlank()
                }
                ?: return@mapNotNull null

            TmdbEpisodeStreamMatch(
                tmdbEpisode = tmdbEpisode,
                streamEpisode = streamEpisode,
            )
        }
}
