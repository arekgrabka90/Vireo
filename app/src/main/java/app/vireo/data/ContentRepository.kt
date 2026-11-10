package app.vireo.data

import app.vireo.data.local.AccountDao
import app.vireo.data.local.AccountEntity
import app.vireo.data.security.CryptoManager
import app.vireo.data.xtream.XtreamCatalogRepository
import app.vireo.domain.model.AccountInfo
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.MovieDetails
import app.vireo.domain.model.SeriesDetails
import app.vireo.domain.model.Title
import app.vireo.domain.provider.ContentProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val dao: AccountDao,
    private val crypto: CryptoManager,
    private val providers: ProviderRegistry,
    private val catalog: XtreamCatalogRepository,
) {
    private val stateMutex = Mutex()

    private var cached: ContentProvider? = null

    @Volatile
    var sourceVersion: Long = 0L
        private set

    suspend fun hasAccount(): Boolean = dao.getActive() != null

    suspend fun activeAccount(): AccountEntity? =
        dao.getActive()?.copy(password = "")

    suspend fun accounts(): List<AccountEntity> =
        dao.getAll().map { it.copy(password = "") }

    suspend fun selectAccount(id: Long): Boolean =
        stateMutex.withLock {
            val selected = dao.activate(id)
            if (selected) {
                cached = null
                clearCachesLocked()
            }
            selected
        }

    suspend fun deleteAccount(id: Long): Boolean =
        stateMutex.withLock {
            if (dao.getById(id) == null) return@withLock false

            dao.deleteAndSelectFallback(id)
            cached = null
            clearCachesLocked()
            true
        }

    private suspend fun providerSnapshot(): Pair<Long, ContentProvider?> =
        stateMutex.withLock {
            cached?.let { return@withLock sourceVersion to it }

            val account = dao.getActive()
                ?: return@withLock sourceVersion to null

            val provider = providers.create(account)

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
        val newProvider = providers.createXtream(server, username.trim(), password)

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
                dao.insertAndActivate(account)
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
            dao.deleteActiveAndSelectFallback()
            cached = null
            clearCachesLocked()
        }
    }

    suspend fun categories(type: ContentType): Result<List<Category>> =
        catalog.categories(type)

    suspend fun titles(
        type: ContentType,
        categoryId: String,
    ): Result<List<Title>> =
        catalog.titles(type, categoryId)

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
        sourceVersion++
    }
}
