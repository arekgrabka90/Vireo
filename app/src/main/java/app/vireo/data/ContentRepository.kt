package app.vireo.data

import app.vireo.data.local.AccountDao
import app.vireo.data.local.AccountEntity
import app.vireo.data.xtream.XtreamProvider
import app.vireo.domain.model.AccountInfo
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.MovieDetails
import app.vireo.domain.model.SeriesDetails
import app.vireo.domain.model.Title
import app.vireo.domain.provider.ContentProvider
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val dao: AccountDao,
    private val client: OkHttpClient,
    private val json: Json,
) {
    private var cached: ContentProvider? = null
    private val categoryCache = mutableMapOf<ContentType, List<Category>>()
    private val titleCache = mutableMapOf<String, List<Title>>()

    /** Rośnie przy każdej zmianie źródła (logowanie/usunięcie), ekrany używają tego do odświeżenia. */
    var sourceVersion: Int = 0
        private set

    suspend fun hasAccount(): Boolean = dao.getActive() != null

    suspend fun activeAccount(): AccountEntity? = dao.getActive()

    suspend fun provider(): ContentProvider? {
        cached?.let { return it }
        val account = dao.getActive() ?: return null
        return XtreamProvider(account.server, account.username, account.password, client, json)
            .also { cached = it }
    }

    suspend fun login(server: String, username: String, password: String): Result<AccountInfo> {
        val provider = XtreamProvider(server, username.trim(), password, client, json)
        val result = provider.authenticate()
        if (result.isSuccess) {
            dao.clear()
            clearCaches()
            dao.insert(
                AccountEntity(
                    name = "Xtream",
                    server = server.trim(),
                    username = username.trim(),
                    password = password,
                ),
            )
            cached = provider
        }
        return result
    }

    suspend fun logout() {
        dao.clear()
        cached = null
        clearCaches()
    }

    suspend fun categories(type: ContentType): Result<List<Category>> {
        categoryCache[type]?.let { return Result.success(it) }
        val p = provider() ?: return Result.failure(NoSourceException())
        return p.categories(type).onSuccess { categoryCache[type] = it }
    }

    suspend fun titles(type: ContentType, categoryId: String): Result<List<Title>> {
        val key = "$type:$categoryId"
        titleCache[key]?.let { return Result.success(it) }
        val p = provider() ?: return Result.failure(NoSourceException())
        val result: Result<List<Title>> = when (type) {
            ContentType.MOVIE -> p.movies(categoryId).map { list ->
                list.map { m -> Title(m.id, m.title, m.posterUrl, m.rating, ContentType.MOVIE, m.categoryId) }
            }
            ContentType.SERIES -> p.series(categoryId).map { list ->
                list.map { s -> Title(s.id, s.title, s.posterUrl, s.rating, ContentType.SERIES, s.categoryId) }
            }
            ContentType.LIVE -> p.channels(categoryId).map { list ->
                list.map { c -> Title(c.id, c.name, c.iconUrl, null, ContentType.LIVE, c.categoryId) }
            }
        }
        return result.onSuccess { titleCache[key] = it }
    }

    suspend fun movieDetails(id: String): Result<MovieDetails> {
        val p = provider() ?: return Result.failure(NoSourceException())
        return p.movieDetails(id)
    }

    suspend fun seriesDetails(id: String): Result<SeriesDetails> {
        val p = provider() ?: return Result.failure(NoSourceException())
        return p.seriesDetails(id)
    }

    suspend fun liveUrl(id: String): String? = provider()?.liveUrl(id)

    private fun clearCaches() {
        categoryCache.clear()
        titleCache.clear()
        sourceVersion++
    }
}
