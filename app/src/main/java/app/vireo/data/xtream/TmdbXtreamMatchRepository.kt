package app.vireo.data.xtream

import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.domain.model.ContentType
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbXtreamMatchRepository @Inject constructor(
    private val indexRepository: XtreamContentIndexRepository,
) {

    /**
     * Matches one TMDB catalog item against every connected Xtream account.
     *
     * Only exact normalized title keys are queried. No fuzzy matching is
     * performed, preventing unrelated titles from becoming false positives.
     */
    suspend fun match(
        item: TmdbCatalogItem,
    ): TmdbXtreamMatch {
        if (item.type == ContentType.LIVE) {
            return TmdbXtreamMatcher.match(item, emptyList())
        }

        val searchTitles = listOfNotNull(
            item.title,
            item.originalTitle,
        )
            .map(XtreamTitleNormalizer::normalize)
            .filter { it.isNotBlank() }
            .distinct()

        val candidates = linkedMapOf<
            Triple<Long, String, String>,
            app.vireo.data.local.XtreamContentIndexEntity
        >()

        for (title in searchTitles) {
            val found = try {
                indexRepository.findCandidates(
                    title = title,
                    type = item.type,
                    year = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }

            for (candidate in found) {
                val key = Triple(
                    candidate.sourceId,
                    candidate.contentType,
                    candidate.streamId,
                )
                candidates[key] = candidate
            }
        }

        return TmdbXtreamMatcher.match(
            item = item,
            candidates = candidates.values,
        )
    }

    /**
     * Processes a catalog collection sequentially.
     * Cancellation is propagated and no partial result is reported as complete.
     */
    suspend fun matchAll(
        items: List<TmdbCatalogItem>,
    ): List<TmdbXtreamMatch> {
        val results = ArrayList<TmdbXtreamMatch>(items.size)

        for (item in items) {
            results += match(item)
        }

        return results
    }
}
