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

    fun match(
        item: TmdbCatalogItem,
        candidates: Collection<XtreamContentIndexEntity>,
    ): TmdbXtreamMatch {
        if (item.type == ContentType.LIVE) {
            return result(item, emptyList())
        }

        val primaryTitle = XtreamTitleNormalizer.normalize(item.title)
        val originalTitle = item.originalTitle
            ?.let(XtreamTitleNormalizer::normalize)
            ?.takeIf { it.isNotBlank() && it != primaryTitle }

        if (primaryTitle.isBlank()) {
            return result(item, emptyList())
        }

        val variants = linkedMapOf<Pair<Long, String>, TmdbXtreamMatchVariant>()

        for (candidate in candidates) {
            if (candidate.contentType != item.type.name) continue
            if (candidate.sourceId <= 0L || candidate.streamId.isBlank()) continue

            val confidenceByTitle = when (candidate.normalizedTitle) {
                primaryTitle -> TmdbXtreamMatchConfidence.EXACT_TITLE
                originalTitle -> TmdbXtreamMatchConfidence.ORIGINAL_TITLE
                else -> null
            } ?: continue

            // Jawny konflikt lat wyklucza kandydata.
            if (
                item.year != null &&
                candidate.year != null &&
                item.year != candidate.year
            ) {
                continue
            }

            // Bez potwierdzonego roku nie tworzymy automatycznego matcha.
            val confidence = if (
                item.year != null && candidate.year == item.year
            ) {
                confidenceByTitle
            } else {
                TmdbXtreamMatchConfidence.NEEDS_REVIEW
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

            if (
                existing == null ||
                confidence.rank() > existing.confidence.rank()
            ) {
                variants[key] = variant
            }
        }

        return result(
            item = item,
            variants = variants.values.sortedWith(
                compareByDescending<TmdbXtreamMatchVariant> {
                    it.confidence.rank()
                }.thenBy { it.sourceId }
                    .thenBy { it.title.lowercase() }
            ),
        )
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

    private fun TmdbXtreamMatchConfidence.rank(): Int = when (this) {
        TmdbXtreamMatchConfidence.EXACT_TITLE -> 3
        TmdbXtreamMatchConfidence.ORIGINAL_TITLE -> 2
        TmdbXtreamMatchConfidence.NEEDS_REVIEW -> 1
    }
}
