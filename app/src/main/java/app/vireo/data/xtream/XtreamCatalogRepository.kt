package app.vireo.data.xtream

import app.vireo.data.ProviderRegistry
import app.vireo.data.local.AccountDao
import app.vireo.data.local.XtreamCatalogCategoryEntity
import app.vireo.data.local.XtreamCatalogDao
import app.vireo.data.local.XtreamCatalogItemEntity
import app.vireo.domain.model.Category
import app.vireo.domain.model.Channel
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Movie
import app.vireo.domain.model.Series
import app.vireo.domain.model.Title
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XtreamCatalogRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val catalogDao: XtreamCatalogDao,
    private val providers: ProviderRegistry,
    @app.vireo.di.ApplicationScope
    private val applicationScope: CoroutineScope,
) {
    private val syncMutex = Mutex()

    suspend fun categories(type: ContentType): Result<List<Category>> {
        val sourceId = ensureCatalog(type).getOrElse {
            return Result.failure(it)
        }

        return catching {
            catalogDao.getCategories(sourceId, type.name).map {
                Category(
                    id = it.categoryId,
                    name = it.name,
                    type = type,
                )
            }
        }
    }

    suspend fun titles(
        type: ContentType,
        categoryId: String,
    ): Result<List<Title>> {
        val sourceId = ensureCatalog(type).getOrElse {
            return Result.failure(it)
        }

        return catching {
            catalogDao.getItems(
                sourceId = sourceId,
                contentType = type.name,
                categoryId = categoryId.takeIf { it.isNotBlank() },
            ).map {
                Title(
                    id = it.itemId,
                    name = it.title,
                    posterUrl = it.posterUrl,
                    rating = it.rating,
                    type = type,
                    categoryId = it.categoryId,
                    epgChannelId = it.epgChannelId,
                )
            }
        }
    }

    private suspend fun ensureCatalog(type: ContentType): Result<Long> {
        val account = accountDao.getActive()
            ?: return Result.failure(IllegalStateException("No active content source"))

        val state = catalogDao.getSyncState(account.id, type.name)

        if (state == null) {
            val oldCategories = catalogDao.countCategories(account.id, type.name)
            val oldItems = catalogDao.countItems(account.id, type.name)

            if (oldCategories == 0 && oldItems == 0) {
                val result = sync(type)
                if (result.isFailure) {
                    return Result.failure(
                        result.exceptionOrNull()
                            ?: IllegalStateException("Catalog synchronization failed")
                    )
                }
            } else {
                applicationScope.launch { sync(type) }
            }
        } else if (
            !isXtreamCatalogFresh(
                lastSuccessfulSyncAt = state.lastSuccessfulSyncAt,
                now = System.currentTimeMillis(),
                ttlMs = CATALOG_TTL_MS,
            )
        ) {
            applicationScope.launch { sync(type) }
        }

        val current = accountDao.getActive()
        if (current?.id != account.id) {
            return Result.failure(
                IllegalStateException("Active content source changed during catalog loading")
            )
        }

        return Result.success(account.id)
    }

    private suspend fun sync(type: ContentType): Result<Unit> = try {
        withContext(Dispatchers.IO) {
            syncMutex.withLock {
                syncLocked(type)
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun syncLocked(type: ContentType): Result<Unit> {
        return try {
        val account = accountDao.getActive()
            ?: error("No active content source")

        val previousState = catalogDao.getSyncState(account.id, type.name)
        if (
            isXtreamCatalogFresh(
                lastSuccessfulSyncAt = previousState?.lastSuccessfulSyncAt,
                now = System.currentTimeMillis(),
                ttlMs = CATALOG_TTL_MS,
            )
        ) {
            return Result.success(Unit)
        }

        val provider = providers.create(account)

        val categories = provider.categories(type).getOrThrow()
        val items = when (type) {
            ContentType.LIVE -> provider.channels(null).getOrThrow().map {
                it.toCatalogEntity(account.id, type, System.currentTimeMillis())
            }
            ContentType.MOVIE -> provider.movies(null).getOrThrow().map {
                it.toCatalogEntity(account.id, System.currentTimeMillis())
            }
            ContentType.SERIES -> provider.series(null).getOrThrow().map {
                it.toCatalogEntity(account.id, System.currentTimeMillis())
            }
        }

        val syncedAt = System.currentTimeMillis()
        val categoryEntities = categories.map {
            it.toCatalogEntity(account.id, syncedAt)
        }

        val oldCategoryCount = catalogDao.countCategories(account.id, type.name)
        val oldItemCount = catalogDao.countItems(account.id, type.name)

        if (
            !shouldReplaceXtreamCatalog(
                previousCategoryCount = oldCategoryCount,
                previousItemCount = oldItemCount,
                incomingCategoryCount = categoryEntities.size,
                incomingItemCount = items.size,
            )
        ) {
            error("Empty Xtream response rejected; previous catalog was preserved")
        }

        if (accountDao.getActive()?.id != account.id) {
            error("Active content source changed; catalog result discarded")
        }

        catalogDao.replaceCatalog(
            sourceId = account.id,
            contentType = type.name,
            categories = categoryEntities,
            items = items,
            syncedAt = syncedAt,
        )

        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
    }

    private fun Category.toCatalogEntity(
        sourceId: Long,
        syncedAt: Long,
    ) = XtreamCatalogCategoryEntity(
        sourceId = sourceId,
        contentType = type.name,
        categoryId = id,
        name = name,
        updatedAt = syncedAt,
    )

    private fun Channel.toCatalogEntity(
        sourceId: Long,
        type: ContentType,
        syncedAt: Long,
    ) = XtreamCatalogItemEntity(
        sourceId = sourceId,
        contentType = type.name,
        itemId = id,
        categoryId = categoryId,
        title = name,
        posterUrl = iconUrl,
        rating = null,
        epgChannelId = epgChannelId,
        tmdbId = null,
        imdbId = null,
        containerExtension = null,
        updatedAt = syncedAt,
    )

    private fun Movie.toCatalogEntity(
        sourceId: Long,
        syncedAt: Long,
    ) = XtreamCatalogItemEntity(
        sourceId = sourceId,
        contentType = ContentType.MOVIE.name,
        itemId = id,
        categoryId = categoryId,
        title = title,
        posterUrl = posterUrl,
        rating = rating,
        epgChannelId = null,
        tmdbId = tmdbId,
        imdbId = imdbId,
        containerExtension = containerExtension,
        updatedAt = syncedAt,
    )

    private fun Series.toCatalogEntity(
        sourceId: Long,
        syncedAt: Long,
    ) = XtreamCatalogItemEntity(
        sourceId = sourceId,
        contentType = ContentType.SERIES.name,
        itemId = id,
        categoryId = categoryId,
        title = title,
        posterUrl = posterUrl,
        rating = rating,
        epgChannelId = null,
        tmdbId = tmdbId,
        imdbId = imdbId,
        containerExtension = null,
        updatedAt = syncedAt,
    )

    private suspend fun <T> catching(
        block: suspend () -> T,
    ): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        private const val CATALOG_TTL_MS = 6 * 60 * 60 * 1000L
    }
}
