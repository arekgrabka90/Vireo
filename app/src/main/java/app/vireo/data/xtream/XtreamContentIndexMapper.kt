package app.vireo.data.xtream

import app.vireo.data.local.XtreamContentIndexEntity
import app.vireo.domain.model.Movie
import app.vireo.domain.model.Series
import java.text.Normalizer
import java.util.Locale

internal object XtreamTitleNormalizer {

    private val yearSuffix = Regex(
        """\s*[\(\[]\s*((?:19|20)\d{2})\s*[\)\]]\s*$"""
    )

    fun extractYear(title: String): Int? =
        yearSuffix.find(title.trim())
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

    fun normalize(title: String): String {
        val trimmed = title.trim()
        val match = yearSuffix.find(trimmed)
        val withoutYear = if (match != null) {
            trimmed.removeRange(match.range).trim()
        } else {
            trimmed
        }

        return Normalizer.normalize(withoutYear, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}

internal object XtreamContentIndexMapper {

    fun movie(
        sourceId: Long,
        item: Movie,
        indexedAt: Long,
    ): XtreamContentIndexEntity? =
        map(
            sourceId = sourceId,
            contentType = "MOVIE",
            streamId = item.id,
            title = item.title,
            posterUrl = item.posterUrl,
            categoryId = item.categoryId,
            indexedAt = indexedAt,
        )

    fun series(
        sourceId: Long,
        item: Series,
        indexedAt: Long,
    ): XtreamContentIndexEntity? =
        map(
            sourceId = sourceId,
            contentType = "SERIES",
            streamId = item.id,
            title = item.title,
            posterUrl = item.posterUrl,
            categoryId = item.categoryId,
            indexedAt = indexedAt,
        )

    private fun map(
        sourceId: Long,
        contentType: String,
        streamId: String,
        title: String,
        posterUrl: String?,
        categoryId: String?,
        indexedAt: Long,
    ): XtreamContentIndexEntity? {
        val cleanId = streamId.trim()
        val cleanTitle = title.trim()

        if (sourceId <= 0L || cleanId.isBlank() || cleanTitle.isBlank()) {
            return null
        }

        val normalized = XtreamTitleNormalizer.normalize(cleanTitle)
        if (normalized.isBlank()) return null

        return XtreamContentIndexEntity(
            sourceId = sourceId,
            contentType = contentType,
            streamId = cleanId,
            title = cleanTitle,
            normalizedTitle = normalized,
            year = XtreamTitleNormalizer.extractYear(cleanTitle),
            posterUrl = posterUrl?.takeIf { it.isNotBlank() },
            categoryId = categoryId?.takeIf { it.isNotBlank() },
            indexedAt = indexedAt,
        )
    }
}
