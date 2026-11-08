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

    private val bracketContent = Regex("""\[[^\]]*]|\([^)]*\)""")
    private val multiSpace = Regex("""\s+""")
    private val seasonToken = Regex(
        """\b(?:s|season)\s*\d{1,2}\b""",
        RegexOption.IGNORE_CASE,
    )
    private val episodeToken = Regex(
        """\b(?:e|ep|episode)\s*\d{1,3}\b""",
        RegexOption.IGNORE_CASE,
    )
    private val releaseTags = Regex(
        """\b(?:2160p|1080p|720p|480p|4k|uhd|fhd|hdr|dv|dovi|hevc|x265|x264|h264|h265|remux|bluray|bdrip|webrip|web[- ]?dl|proper|repack|multi|dubbed|dual[- ]?audio)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val languagePrefix = Regex(
        """^(?:de|deu|ger|en|eng|fr|fra|fre|es|esp|spa|pt|por|nl|dut|pl|pol|tr|tur|ar|ara|ru|rus|ro|ron|rom|ita|ell|gre|cz|cze|hu|hun|swe|nor|dan|fin|bg|bul|hr|hrv|srp|sk|slo|slv|ua|ukr|mk|mkd|vip|multi|dual)\s*[:\-|]\s*""",
        RegexOption.IGNORE_CASE,
    )
    private val nonAlphaNumeric = Regex("""[^\p{L}\p{N}]+""")
    private val diacritics = Regex("""\p{Mn}+""")

    fun extractYear(title: String): Int? =
        yearSuffix.find(title.trim())
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

    fun normalize(title: String): String {
        if (title.isBlank()) return ""

        val stripped = title
            .replace('\u2503', '|')
            .replace(Regex("""^\s*\|(?:[A-Za-z0-9]{1,6}\|)+\s*"""), " ")
            .trimStart()
            .replace(languagePrefix, " ")
            .replace(bracketContent, " ")
            .replace(seasonToken, " ")
            .replace(episodeToken, " ")
            .replace(releaseTags, " ")

        return Normalizer.normalize(stripped, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(nonAlphaNumeric, " ")
            .trim()
            .replace(multiSpace, " ")
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
