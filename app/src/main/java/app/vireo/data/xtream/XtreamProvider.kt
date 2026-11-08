package app.vireo.data.xtream

import app.vireo.domain.model.AccountInfo
import app.vireo.domain.model.Category
import app.vireo.domain.model.Channel
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Movie
import app.vireo.domain.model.Series
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
        .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
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
                streamUrl = "$base/live/$username/$password/$id.m3u8",
            )
        }
    }

    override suspend fun movies(categoryId: String?): Result<List<Movie>> = runCatching {
        fetch("get_vod_streams", categoryParams(categoryId)).objects().mapNotNull { o ->
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
        fetch("get_series", categoryParams(categoryId)).objects().mapNotNull { o ->
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

    private fun JsonElement.objects(): List<JsonObject> =
        (this as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
