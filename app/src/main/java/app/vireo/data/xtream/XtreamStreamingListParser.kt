package app.vireo.data.xtream

import app.vireo.domain.model.Channel
import app.vireo.domain.model.Movie
import app.vireo.domain.model.Series
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okio.BufferedSource

private val STREAMING_IMDB_ID_PATTERN = Regex("(?i)^tt[0-9]{5,12}$")

internal data class XtreamListParseSummary(
    val decoded: Int,
    val skipped: Int,
    val end: JsonArrayEnd,
) {
    /** False for TRUNCATED, OVERSIZED, EMPTY and NOT_ARRAY: callers must not replace stored data. */
    val isComplete: Boolean get() = end == JsonArrayEnd.COMPLETE
}

/**
 * E2.1: streaming list parser (variant B), placed beside the existing
 * XtreamProvider path and not wired to it. Mapping mirrors XtreamProvider.
 * Difference by design: a row that does not map (non-object, malformed,
 * no id) is skipped and counted instead of failing the whole list.
 * Blocking; run on an IO dispatcher.
 */
internal class XtreamStreamingListParser(
    server: String,
    private val username: String,
    private val password: String,
    private val json: Json,
    private val maxElementBytes: Long = JsonArrayElementSplitter.DEFAULT_MAX_ELEMENT_BYTES,
) {
    private val base: String = server.trim()
        .let { if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it" }
        .trimEnd('/')

    fun parseChannels(source: BufferedSource, onChannel: (Channel) -> Unit): XtreamListParseSummary =
        parse(source, this::toChannel, onChannel)

    fun parseMovies(source: BufferedSource, onMovie: (Movie) -> Unit): XtreamListParseSummary =
        parse(source, this::toMovie, onMovie)

    fun parseSeries(source: BufferedSource, onSeries: (Series) -> Unit): XtreamListParseSummary =
        parse(source, this::toSeries, onSeries)

    private fun <T : Any> parse(
        source: BufferedSource,
        map: (JsonObject) -> T?,
        onItem: (T) -> Unit,
    ): XtreamListParseSummary {
        var decoded = 0
        var skipped = 0
        val end = JsonArrayElementSplitter(source, maxElementBytes).forEachElement { text ->
            val item = parseObject(text)?.let(map)
            if (item == null) {
                skipped++
            } else {
                decoded++
                onItem(item)
            }
        }
        return XtreamListParseSummary(decoded, skipped, end)
    }

    private fun parseObject(text: String): JsonObject? =
        try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: IllegalArgumentException) {
            null // SerializationException extends IllegalArgumentException
        }

    private fun toChannel(o: JsonObject): Channel? {
        val id = o.str("stream_id") ?: return null
        return Channel(
            id = id,
            name = o.str("name") ?: id,
            iconUrl = o.str("stream_icon"),
            categoryId = o.str("category_id"),
            epgChannelId = o.str("epg_channel_id"),
            streamUrl = "$base/live/$username/$password/$id.m3u8",
        )
    }

    private fun toMovie(o: JsonObject): Movie? {
        val id = o.str("stream_id") ?: return null
        val ext = o.str("container_extension") ?: "mp4"
        return Movie(
            id = id,
            title = o.str("name") ?: id,
            posterUrl = o.str("stream_icon"),
            categoryId = o.str("category_id"),
            rating = o.str("rating"),
            streamUrl = "$base/movie/$username/$password/$id.$ext",
            tmdbId = o.firstInt("tmdb_id", "tmdb", "tmdbid")?.takeIf { it > 0 },
            imdbId = o.firstString("imdb_id", "imdb", "imdbid")
                ?.takeIf { STREAMING_IMDB_ID_PATTERN.matches(it) }
                ?.lowercase(),
            containerExtension = ext,
        )
    }

    private fun toSeries(o: JsonObject): Series? {
        val id = o.str("series_id") ?: return null
        return Series(
            id = id,
            title = o.str("name") ?: id,
            posterUrl = o.str("cover"),
            categoryId = o.str("category_id"),
            rating = o.str("rating"),
            tmdbId = o.firstInt("tmdb_id", "tmdb", "tmdbid")?.takeIf { it > 0 },
            imdbId = o.firstString("imdb_id", "imdb", "imdbid")
                ?.takeIf { STREAMING_IMDB_ID_PATTERN.matches(it) }
                ?.lowercase(),
        )
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.firstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> str(key)?.trim()?.takeIf { it.isNotEmpty() } }

    private fun JsonObject.firstInt(vararg keys: String): Int? =
        firstString(*keys)?.toIntOrNull()
}
