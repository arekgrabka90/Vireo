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
    EXACT_ID,
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
    private val imdbIdPattern = Regex("(?i)^tt[0-9]{5,12}$")

    fun match(
        item: TmdbCatalogItem,
        candidates: Collection<XtreamContentIndexEntity>,
    ): TmdbXtreamMatch {
        if (item.type == ContentType.LIVE) return result(item, emptyList())

        val primary = XtreamTitleNormalizer.normalize(item.title)
        val original = item.originalTitle
            ?.let(XtreamTitleNormalizer::normalize)
            ?.takeIf { it.isNotBlank() && it != primary }
        val wantedImdb = item.imdbId?.trim()?.lowercase()
            ?.takeIf { imdbIdPattern.matches(it) }

        val variants = linkedMapOf<Pair<Long, String>, Pair<TmdbXtreamMatchVariant, Int>>()

        for (candidate in candidates) {
            if (candidate.contentType != item.type.name) continue
            if (candidate.sourceId <= 0L || candidate.streamId.isBlank()) continue

            val exactTmdb = item.tmdbId > 0 && candidate.tmdbId == item.tmdbId
            val candidateImdb = candidate.imdbId?.trim()?.lowercase()
                ?.takeIf { imdbIdPattern.matches(it) }
            val exactImdb = wantedImdb != null && candidateImdb == wantedImdb

            // Either matching external ID is sufficient, even if the other ID
            // disagrees. The matching identifier is stronger evidence.
            if (exactTmdb || exactImdb) {
                putVariant(
                    variants = variants,
                    candidate = candidate,
                    confidence = TmdbXtreamMatchConfidence.EXACT_ID,
                    score = 10_000,
                )
                continue
            }

            // Do not let title similarity override contradictory populated IDs.
            val tmdbConflict = item.tmdbId > 0 && candidate.tmdbId != null &&
                candidate.tmdbId > 0 && candidate.tmdbId != item.tmdbId
            val imdbConflict = wantedImdb != null && candidateImdb != null &&
                candidateImdb != wantedImdb
            if (tmdbConflict || imdbConflict) continue

            // An exact external ID can still match when the title is blank.
            if (primary.isBlank()) continue

            // Re-normalize raw provider title because persisted rows may have
            // been indexed by an older normalizer and remain inside TTL.
            val providerTitle = XtreamTitleNormalizer.normalize(candidate.title)
            if (providerTitle.isBlank()) continue

            val primaryScore = scoreNameMatch(providerTitle, primary)
            val originalScore = original?.let { scoreNameMatch(providerTitle, it) } ?: 0
            val score = maxOf(primaryScore, originalScore)
            if (score < MIN_MATCH_SCORE) continue

            val providerYear = candidate.year
                ?: XtreamTitleNormalizer.extractYear(candidate.title)
            if (
                item.year != null && providerYear != null &&
                kotlin.math.abs(item.year - providerYear) > 1
            ) continue

            val exactPrimary = providerTitle == primary
            val exactOriginal = original != null && providerTitle == original
            val sameYear = item.year != null && providerYear == item.year
            val confidence = when {
                exactPrimary && sameYear -> TmdbXtreamMatchConfidence.EXACT_TITLE
                exactOriginal && sameYear -> TmdbXtreamMatchConfidence.ORIGINAL_TITLE
                else -> TmdbXtreamMatchConfidence.NEEDS_REVIEW
            }
            val adjustedScore = when {
                item.year == null || providerYear == null -> score
                item.year == providerYear -> score + 20
                else -> score + 8
            }
            putVariant(variants, candidate, confidence, adjustedScore)
        }

        val sorted = variants.values
            .sortedWith(
                compareByDescending<Pair<TmdbXtreamMatchVariant, Int>> { it.second }
                    .thenBy { it.first.sourceId }
                    .thenBy { it.first.title.lowercase() }
            )
            .map { it.first }
        return result(item, sorted)
    }

    private fun putVariant(
        variants: MutableMap<Pair<Long, String>, Pair<TmdbXtreamMatchVariant, Int>>,
        candidate: XtreamContentIndexEntity,
        confidence: TmdbXtreamMatchConfidence,
        score: Int,
    ) {
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
        val previous = variants[key]
        if (previous == null || score > previous.second) variants[key] = variant to score
    }

    private fun scoreNameMatch(provider: String, wanted: String): Int {
        if (provider.isBlank() || wanted.isBlank()) return 0
        if (provider == wanted) return 120
        if (
            wanted.length >= 5 &&
            Regex("(^| )${Regex.escape(wanted)}( |$)").containsMatchIn(provider)
        ) return 90

        val providerWords = provider.split(' ').filter { it.isNotBlank() && it !in stopWords }.toSet()
        val wantedWords = wanted.split(' ').filter { it.isNotBlank() && it !in stopWords }.toSet()
        if (providerWords.isEmpty() || wantedWords.isEmpty()) return 0
        val overlap = providerWords.intersect(wantedWords).size
        val coverage = overlap.toDouble() / wantedWords.size.toDouble()
        return when {
            overlap >= 2 && coverage >= 0.75 -> 75 + overlap
            overlap >= 2 && wantedWords.size >= 3 && coverage >= 0.60 -> 68 + overlap
            else -> 0
        }
    }

    private fun result(
        item: TmdbCatalogItem,
        variants: List<TmdbXtreamMatchVariant>,
    ): TmdbXtreamMatch {
        val status = when {
            variants.any { it.confidence != TmdbXtreamMatchConfidence.NEEDS_REVIEW } ->
                TmdbXtreamMatchStatus.MATCHED
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
