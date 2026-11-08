package app.vireo.data.xtream

import app.vireo.domain.model.AccountInfo
import app.vireo.domain.model.Category
import app.vireo.domain.model.Channel
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Episode
import app.vireo.domain.model.Movie
import app.vireo.domain.model.MovieDetails
import app.vireo.domain.model.Season
import app.vireo.domain.model.Series
import app.vireo.domain.model.SeriesDetails
import app.vireo.domain.provider.ContentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class XtreamProvider(
    server: String,
    private val username: String,
    private val password: String,
    private val client: OkHttpClient,
    private val json: Json,
) : ContentProvider {

    private val base: String = server.trim()
        .let { if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it" }
        .trimEnd('/')

    override suspend fun authenticate(): Result<AccountInfo> = runCatching {
        val root = fetch(null) as? JsonObject ?: error("Invalid server response")
        val info = root["user_info"] as? JsonObject ?: error("Login failed")
        if (info.str("auth") == "0") error("Invalid credentials")
        AccountInfo(
            status = info.str("status") ?: "unknown",
            expiresAt = info.str("exp_date")?.toLongOrNull(),
            maxConnections = info.str("max_connections")?.toIntOrNull(),
        )
    }

    override suspend fun categories(type: ContentType): Result<List<Category>> = runCatching {
        val action = when (type) {
            ContentType.LIVE -> "get_live_categories"
            ContentType.MOVIE -> "get_vod_categories"
            ContentType.SERIES -> "get_series_categories"
        }
        fetch(action).objects().mapNotNull { o ->
            val id = o.str("category_id") ?: return@mapNotNull null
            Category(id = id, name = o.str("category_name") ?: id, type = type)
        }
    }

    override suspend fun channels(categoryId: String?): Result<List<Channel>> = runCatching {
        fetch("get_live_streams", categoryParams(categoryId)).objects().mapNotNull { o ->
            val id = o.str("stream_id") ?: return@mapNotNull null
            Channel(
                id = id,
                name = o.str("name") ?: id,
                iconUrl = o.str("stream_icon"),
                categoryId = o.str("category_id"),
                epgChannelId = o.str("epg_channel_id"),
                streamUrl = liveUrl(id),
            )
        }
    }

    override suspend fun movies(categoryId: String?): Result<List<Movie>> = runCatching {
        fetch("get_vod_streams", categoryParams(categoryId)).strictObjects().mapNotNull { o ->
            val id = o.str("stream_id") ?: return@mapNotNull null
            val ext = o.str("container_extension") ?: "mp4"
            Movie(
                id = id,
                title = o.str("name") ?: id,
                posterUrl = o.str("stream_icon"),
                categoryId = o.str("category_id"),
                rating = o.str("rating"),
                streamUrl = "$base/movie/$username/$password/$id.$ext",
            )
        }
    }

    override suspend fun series(categoryId: String?): Result<List<Series>> = runCatching {
        fetch("get_series", categoryParams(categoryId)).strictObjects().mapNotNull { o ->
            val id = o.str("series_id") ?: return@mapNotNull null
            Series(
                id = id,
                title = o.str("name") ?: id,
                posterUrl = o.str("cover"),
                categoryId = o.str("category_id"),
                rating = o.str("rating"),
            )
        }
    }

    override suspend fun movieDetails(id: String): Result<MovieDetails> = runCatching {
        val root = fetch("get_vod_info", mapOf("vod_id" to id)) as? JsonObject
            ?: error("Invalid server response")
        val info = root["info"] as? JsonObject
        val data = root["movie_data"] as? JsonObject
        val ext = data?.str("container_extension") ?: "mp4"
        MovieDetails(
            id = id,
            name = info?.str("name") ?: data?.str("name") ?: id,
            posterUrl = info?.str("movie_image") ?: info?.str("cover_big"),
            backdropUrl = info?.firstUrl("backdrop_path"),
            plot = info?.str("plot") ?: info?.str("description"),
            genre = info?.str("genre"),
            year = (info?.str("releasedate") ?: info?.str("release_date") ?: info?.str("year"))?.take(4),
            duration = info?.str("duration"),
            cast = info?.str("cast") ?: info?.str("actors"),
            director = info?.str("director"),
            rating = info?.str("rating"),
            streamUrl = "$base/movie/$username/$password/$id.$ext",
        )
    }

    override suspend fun seriesDetails(id: String): Result<SeriesDetails> = runCatching {
        val root = fetch("get_series_info", mapOf("series_id" to id)) as? JsonObject
            ?: error("Invalid server response")
        val info = root["info"] as? JsonObject
        val eps = root["episodes"]
        val seasonArrays: List<Pair<Int, JsonArray>> = when (eps) {
            is JsonObject -> eps.entries.mapNotNull { e ->
                val n = e.key.toIntOrNull()
                val arr = e.value as? JsonArray
                if (n != null && arr != null) n to arr else null
            }
            is JsonArray -> eps.mapIndexedNotNull { i, v ->
                (v as? JsonArray)?.let { arr -> (i + 1) to arr }
            }
            else -> emptyList()
        }
        val seasons = seasonArrays.map { (number, arr) ->
            val episodes = arr.mapNotNull { it as? JsonObject }.mapNotNull { o ->
                val epId = o.str("id") ?: return@mapNotNull null
                val epInfo = o["info"] as? JsonObject
                val ext = o.str("container_extension") ?: "mp4"
                Episode(
                    id = epId,
                    number = o.str("episode_num")?.toIntOrNull() ?: 0,
                    title = o.str("title") ?: "Episode",
                    plot = epInfo?.str("plot"),
                    duration = epInfo?.str("duration"),
                    thumbUrl = epInfo?.str("movie_image"),
                    streamUrl = "$base/series/$username/$password/$epId.$ext",
                )
            }.sortedBy { it.number }
            Season(number, episodes)
        }.filter { it.episodes.isNotEmpty() }.sortedBy { it.number }
        SeriesDetails(
            id = id,
            name = info?.str("name") ?: id,
            posterUrl = info?.str("cover"),
            backdropUrl = info?.firstUrl("backdrop_path"),
            plot = info?.str("plot") ?: info?.str("description"),
            genre = info?.str("genre"),
            year = (info?.str("releaseDate") ?: info?.str("releasedate") ?: info?.str("release_date"))?.take(4),
            cast = info?.str("cast"),
            rating = info?.str("rating"),
            seasons = seasons,
        )
    }

    override fun liveUrl(id: String): String = "$base/live/$username/$password/$id.m3u8"

    private fun categoryParams(categoryId: String?): Map<String, String> =
        categoryId?.let { mapOf("category_id" to it) } ?: emptyMap()

    private suspend fun fetch(
        action: String?,
        params: Map<String, String> = emptyMap(),
    ): JsonElement = withContext(Dispatchers.IO) {
        val url = "$base/player_api.php".toHttpUrl().newBuilder()
            .addQueryParameter("username", username)
            .addQueryParameter("password", password)
            .apply {
                action?.let { addQueryParameter("action", it) }
                params.forEach { (k, v) -> addQueryParameter(k, v) }
            }
            .build()
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            json.parseToJsonElement(resp.body?.string().orEmpty())
        }
    }

    private fun JsonElement.strictObjects(): List<JsonObject> {
        val array = this as? JsonArray
            ?: throw IOException("Invalid Xtream list response")
        if (array.any { it !is JsonObject }) {
            throw IOException("Invalid Xtream list response")
        }
        return array.map { it as JsonObject }
    }

    private fun JsonElement.objects(): List<JsonObject> =
        (this as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.firstUrl(key: String): String? = when (val v = this[key]) {
        is JsonArray -> v.firstNotNullOfOrNull { el ->
            (el as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() }
        }
        is JsonPrimitive -> v.contentOrNull?.takeIf { it.isNotBlank() }
        else -> null
    }
}
