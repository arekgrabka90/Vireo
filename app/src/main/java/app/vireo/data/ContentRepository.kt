package app.vireo.data

import app.vireo.data.local.AccountDao
import app.vireo.data.local.AccountEntity
import app.vireo.data.security.CryptoManager
import app.vireo.data.xtream.XtreamProvider
import app.vireo.domain.model.AccountInfo
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.MovieDetails
import app.vireo.domain.model.SeriesDetails
import app.vireo.domain.model.Title
import app.vireo.domain.provider.ContentProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val dao: AccountDao,
    private val client: OkHttpClient,
    private val json: Json,
    private val crypto: CryptoManager,
) {
    private val stateMutex = Mutex()

    private var cached: ContentProvider? = null
    private val categoryCache = mutableMapOf<ContentType, List<Category>>()
    private val titleCache = mutableMapOf<String, List<Title>>()

    @Volatile
    var sourceVersion: Long = 0L
        private set

    suspend fun hasAccount(): Boolean = dao.getActive() != null

    suspend fun activeAccount(): AccountEntity? =
        dao.getActive()?.copy(password = "")

    private suspend fun providerSnapshot(): Pair<Long, ContentProvider?> =
        stateMutex.withLock {
            cached?.let { return@withLock sourceVersion to it }

            val account = dao.getActive()
                ?: return@withLock sourceVersion to null

            val provider = XtreamProvider(
                account.server,
                account.username,
                crypto.decrypt(account.password),
                client,
                json,
            )

            cached = provider
            sourceVersion to provider
        }

    suspend fun provider(): ContentProvider? =
        providerSnapshot().second

    private suspend fun <T> finishRequest(
        version: Long,
        result: Result<T>,
        onSuccess: ((T) -> Unit)? = null,
    ): Result<T> = stateMutex.withLock {
        if (version != sourceVersion) {
            Result.failure(StaleSourceException())
        } else {
            result.onSuccess { value -> onSuccess?.invoke(value) }
        }
    }

    suspend fun login(
        server: String,
        username: String,
        password: String,
    ): Result<AccountInfo> {
        val newProvider = XtreamProvider(
            server,
            username.trim(),
            password,
            client,
            json,
        )

        val result = newProvider.authenticate()
        if (result.isFailure) return result

        val encryptedPassword = try {
            crypto.encrypt(password)
        } catch (error: Exception) {
            return Result.failure(error)
        }

        val account = AccountEntity(
            name = "Xtream",
            server = server.trim(),
            username = username.trim(),
            password = encryptedPassword,
        )

        return try {
            stateMutex.withLock {
                dao.replaceAccount(account)
                cached = newProvider
                clearCachesLocked()
            }
            result
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun logout() {
        stateMutex.withLock {
            dao.clear()
            cached = null
            clearCachesLocked()
        }
    }

    suspend fun categories(type: ContentType): Result<List<Category>> {
        val cachedResult = stateMutex.withLock {
            categoryCache[type]?.let { Result.success(it) }
        }
        if (cachedResult != null) return cachedResult

        val (version, provider) = providerSnapshot()
        if (provider == null) {
            return Result.failure(NoSourceException())
        }

        val result = provider.categories(type)

        return finishRequest(version, result) { categories ->
            categoryCache[type] = categories
        }
    }

    suspend fun titles(
        type: ContentType,
        categoryId: String,
    ): Result<List<Title>> {
        val key = "$type:$categoryId"

        val cachedResult = stateMutex.withLock {
            titleCache[key]?.let { Result.success(it) }
        }
        if (cachedResult != null) return cachedResult

        val (version, provider) = providerSnapshot()
        if (provider == null) {
            return Result.failure(NoSourceException())
        }

        val result: Result<List<Title>> = when (type) {
            ContentType.MOVIE -> provider.movies(categoryId).map { list ->
                list.map { item ->
                    Title(
                        item.id,
                        item.title,
                        item.posterUrl,
                        item.rating,
                        ContentType.MOVIE,
                        item.categoryId,
                    )
                }
            }

            ContentType.SERIES -> provider.series(categoryId).map { list ->
                list.map { item ->
                    Title(
                        item.id,
                        item.title,
                        item.posterUrl,
                        item.rating,
                        ContentType.SERIES,
                        item.categoryId,
                    )
                }
            }

            ContentType.LIVE -> provider.channels(categoryId).map { list ->
                list.map { item ->
                    Title(
                        item.id,
                        item.name,
                        item.iconUrl,
                        null,
                        ContentType.LIVE,
                        item.categoryId,
                    )
                }
            }
        }

        return finishRequest(version, result) { titles ->
            titleCache[key] = titles
        }
    }

    suspend fun movieDetails(id: String): Result<MovieDetails> {
        val (version, provider) = providerSnapshot()
        if (provider == null) {
            return Result.failure(NoSourceException())
        }
        return finishRequest(version, provider.movieDetails(id))
    }

    suspend fun seriesDetails(id: String): Result<SeriesDetails> {
        val (version, provider) = providerSnapshot()
        if (provider == null) {
            return Result.failure(NoSourceException())
        }
        return finishRequest(version, provider.seriesDetails(id))
    }

    suspend fun liveUrl(id: String): String? {
        val (version, provider) = providerSnapshot()
        if (provider == null) return null

        val url = provider.liveUrl(id)

        return stateMutex.withLock {
            if (version == sourceVersion) url else null
        }
    }

    private fun clearCachesLocked() {
        categoryCache.clear()
        titleCache.clear()
        sourceVersion++
    }
}
