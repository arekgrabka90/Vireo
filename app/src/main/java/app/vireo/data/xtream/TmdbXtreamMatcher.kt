package app.vireo.data.xtream

import app.vireo.data.local.XtreamContentIndexEntity
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.domain.model.ContentType

enum class TmdbXtreamMatchStatus {
    MATCHED,
    NEEDS_REVIEW,
    NOT_FOUND,
}

enum class TmdbXtreamMatchConfidence {
    EXACT_TITLE,
    ORIGINAL_TITLE,
    NEEDS_REVIEW,
}

data class TmdbXtreamMatchVariant(
    val sourceId: Long,
    val streamId: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String?,
    val indexedAt: Long,
    val confidence: TmdbXtreamMatchConfidence,
)

data class TmdbXtreamMatch(
    val tmdbId: Int,
    val contentType: ContentType,
    val title: String,
    val year: Int?,
    val status: TmdbXtreamMatchStatus,
    val variants: List<TmdbXtreamMatchVariant>,
)

internal object TmdbXtreamMatcher {

    private val stopWords = setOf(
        "the", "a", "an", "and", "of", "to", "in", "on",
        "part", "episode", "season", "movie",
    )

    fun match(
        item: TmdbCatalogItem,
        candidates: Collection<XtreamContentIndexEntity>,
    ): TmdbXtreamMatch {
        if (item.type == ContentType.LIVE) return result(item, emptyList())

        val primary = XtreamTitleNormalizer.normalize(item.title)
        val original = item.originalTitle
            ?.let(XtreamTitleNormalizer::normalize)
            ?.takeIf { it.isNotBlank() && it != primary }

        if (primary.isBlank()) return result(item, emptyList())

        val variants = linkedMapOf<Pair<Long, String>, Pair<TmdbXtreamMatchVariant, Int>>()

        for (candidate in candidates) {
            if (candidate.contentType != item.type.name) continue
            if (candidate.sourceId <= 0L || candidate.streamId.isBlank()) continue

            // Re-normalize the raw provider title. Existing database rows may
            // have been indexed by an older normalizer and remain inside TTL.
            val providerTitle = XtreamTitleNormalizer.normalize(candidate.title)
            if (providerTitle.isBlank()) continue

            val primaryScore = scoreNameMatch(providerTitle, primary)
            val originalScore = original?.let { scoreNameMatch(providerTitle, it) } ?: 0
            val score = maxOf(primaryScore, originalScore)
            if (score < MIN_MATCH_SCORE) continue

            val providerYear = candidate.year
                ?: XtreamTitleNormalizer.extractYear(candidate.title)

            // A substantial year conflict is not a match (e.g. different
            // remakes). A one-year discrepancy is retained only for review.
            if (
                item.year != null &&
                providerYear != null &&
                kotlin.math.abs(item.year - providerYear) > 1
            ) {
                continue
            }

            val exactPrimary = providerTitle == primary
            val exactOriginal = original != null && providerTitle == original
            val sameYear = item.year != null && providerYear == item.year

            val confidence = when {
                exactPrimary && sameYear -> TmdbXtreamMatchConfidence.EXACT_TITLE
                exactOriginal && sameYear -> TmdbXtreamMatchConfidence.ORIGINAL_TITLE
                else -> TmdbXtreamMatchConfidence.NEEDS_REVIEW
            }

            val yearAdjustedScore = when {
                item.year == null || providerYear == null -> score
                item.year == providerYear -> score + 20
                else -> score + 8
            }

            val variant = TmdbXtreamMatchVariant(
                sourceId = candidate.sourceId,
                streamId = candidate.streamId,
                title = candidate.title,
                posterUrl = candidate.posterUrl,
                categoryId = candidate.categoryId,
                indexedAt = candidate.indexedAt,
                confidence = confidence,
            )

            val key = candidate.sourceId to candidate.streamId
            val existing = variants[key]
            if (existing == null || yearAdjustedScore > existing.second) {
                variants[key] = variant to yearAdjustedScore
            }
        }

        val sorted = variants.values
            .sortedWith(
                compareByDescending<Pair<TmdbXtreamMatchVariant, Int>> {
                    it.second
                }.thenBy { it.first.sourceId }
                    .thenBy { it.first.title.lowercase() }
            )
            .map { it.first }

        return result(item, sorted)
    }

    private fun scoreNameMatch(
        provider: String,
        wanted: String,
    ): Int {
        if (provider.isBlank() || wanted.isBlank()) return 0
        if (provider == wanted) return 120

        // Provider titles often add a subtitle or edition label.
        if (
            wanted.length >= 5 &&
            Regex("(^| )${Regex.escape(wanted)}( |$)").containsMatchIn(provider)
        ) return 90

        val providerWords = provider.split(' ')
            .filter { it.isNotBlank() && it !in stopWords }
            .toSet()
        val wantedWords = wanted.split(' ')
            .filter { it.isNotBlank() && it !in stopWords }
            .toSet()

        if (providerWords.isEmpty() || wantedWords.isEmpty()) return 0

        val overlap = providerWords.intersect(wantedWords).size
        val coverage = overlap.toDouble() / wantedWords.size.toDouble()

        return when {
            overlap >= 2 && coverage >= 0.75 -> 75 + overlap
            overlap >= 2 && wantedWords.size >= 3 && coverage >= 0.60 -> 68 + overlap
            // A one-word partial title is too ambiguous to accept.
            else -> 0
        }
    }

    private fun result(
        item: TmdbCatalogItem,
        variants: List<TmdbXtreamMatchVariant>,
    ): TmdbXtreamMatch {
        val status = when {
            variants.any {
                it.confidence != TmdbXtreamMatchConfidence.NEEDS_REVIEW
            } -> TmdbXtreamMatchStatus.MATCHED

            variants.isNotEmpty() -> TmdbXtreamMatchStatus.NEEDS_REVIEW
            else -> TmdbXtreamMatchStatus.NOT_FOUND
        }

        return TmdbXtreamMatch(
            tmdbId = item.tmdbId,
            contentType = item.type,
            title = item.title,
            year = item.year,
            status = status,
            variants = variants,
        )
    }

    private const val MIN_MATCH_SCORE = 65
}
