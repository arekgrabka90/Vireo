package app.vireo.data.xtream

import app.vireo.data.ProviderRegistry
import app.vireo.data.local.AccountDao
import app.vireo.data.local.CatalogSyncStateEntity
import app.vireo.data.local.XtreamContentIndexDao
import app.vireo.data.local.XtreamContentIndexEntity
import app.vireo.domain.model.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class XtreamIndexSyncFailure(
    val sourceId: Long,
    val contentType: ContentType,
    val message: String,
)

data class XtreamIndexSyncReport(
    val accountCount: Int,
    val indexedMovieCount: Int,
    val indexedSeriesCount: Int,
    val successfulMovieSources: Int,
    val successfulSeriesSources: Int,
    val failures: List<XtreamIndexSyncFailure>,
) {
    val isSuccessful: Boolean
        get() = failures.isEmpty()
}

internal fun isXtreamIndexFresh(
    lastSuccessfulSyncAt: Long?,
    now: Long,
    ttlMs: Long,
): Boolean {
    if (lastSuccessfulSyncAt == null || ttlMs <= 0L) return false
    if (lastSuccessfulSyncAt > now) return false

    return now - lastSuccessfulSyncAt < ttlMs
}

@Singleton
class XtreamContentIndexRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val indexDao: XtreamContentIndexDao,
    private val providers: ProviderRegistry,
) {
    private val movieSyncMutex = Mutex()
    private val seriesSyncMutex = Mutex()

    suspend fun syncMovies(
        forceRefresh: Boolean = false,
    ): XtreamIndexSyncReport = sync(ContentType.MOVIE, forceRefresh)

    suspend fun syncSeries(
        forceRefresh: Boolean = false,
    ): XtreamIndexSyncReport = sync(ContentType.SERIES, forceRefresh)

    suspend fun syncAll(
        forceRefresh: Boolean = false,
    ): XtreamIndexSyncReport {
        val movies = syncMovies(forceRefresh)
        val series = syncSeries(forceRefresh)

        return XtreamIndexSyncReport(
            accountCount = maxOf(movies.accountCount, series.accountCount),
            indexedMovieCount = movies.indexedMovieCount,
            indexedSeriesCount = series.indexedSeriesCount,
            successfulMovieSources = movies.successfulMovieSources,
            successfulSeriesSources = series.successfulSeriesSources,
            failures = movies.failures + series.failures,
        )
    }

    private suspend fun sync(
        type: ContentType,
        forceRefresh: Boolean,
    ): XtreamIndexSyncReport {
        if (type == ContentType.LIVE) return emptyReport()

        val mutex = when (type) {
            ContentType.MOVIE -> movieSyncMutex
            ContentType.SERIES -> seriesSyncMutex
            ContentType.LIVE -> return emptyReport()
        }

        return mutex.withLock {
            syncLocked(type, forceRefresh)
        }
    }

    private suspend fun syncLocked(
        type: ContentType,
        forceRefresh: Boolean,
    ): XtreamIndexSyncReport {
        val accounts = accountDao.getAll()
        val failures = mutableListOf<XtreamIndexSyncFailure>()

        var movieCount = 0
        var seriesCount = 0
        var movieSources = 0
        var seriesSources = 0

        for (account in accounts) {
            try {
                val previousState = indexDao.getSyncState(account.id, type.name)
                val now = System.currentTimeMillis()

                if (
                    !forceRefresh &&
                    isXtreamIndexFresh(
                        lastSuccessfulSyncAt = previousState?.lastSuccessfulSyncAt,
                        now = now,
                        ttlMs = INDEX_TTL_MS,
                    )
                ) {
                    val existingCount = indexDao
                        .getForSource(account.id, type.name)
                        .size

                    when (type) {
                        ContentType.MOVIE -> {
                            movieCount += existingCount
                            movieSources++
                        }
                        ContentType.SERIES -> {
                            seriesCount += existingCount
                            seriesSources++
                        }
                        ContentType.LIVE -> Unit
                    }
                    continue
                }

                val provider = try {
                    providers.create(account)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failures += failure(account.id, type)
                    continue
                }

                when (type) {
                    ContentType.MOVIE -> {
                        val result = request { provider.movies(null) }

                        if (result.isSuccess) {
                            try {
                                val syncedAt = System.currentTimeMillis()
                                val entries = result.getOrThrow().mapNotNull {
                                    XtreamContentIndexMapper.movie(
                                        account.id,
                                        it,
                                        syncedAt,
                                    )
                                }

                                indexDao.replaceForSourceAndType(
                                    sourceId = account.id,
                                    contentType = type.name,
                                    items = entries,
                                    syncedAt = syncedAt,
                                )

                                movieCount += entries.size
                                movieSources++
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failures += failure(account.id, type)
                            }
                        } else {
                            failures += failure(account.id, type)
                        }
                    }

                    ContentType.SERIES -> {
                        val result = request { provider.series(null) }

                        if (result.isSuccess) {
                            try {
                                val syncedAt = System.currentTimeMillis()
                                val entries = result.getOrThrow().mapNotNull {
                                    XtreamContentIndexMapper.series(
                                        account.id,
                                        it,
                                        syncedAt,
                                    )
                                }

                                indexDao.replaceForSourceAndType(
                                    sourceId = account.id,
                                    contentType = type.name,
                                    items = entries,
                                    syncedAt = syncedAt,
                                )

                                seriesCount += entries.size
                                seriesSources++
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failures += failure(account.id, type)
                            }
                        } else {
                            failures += failure(account.id, type)
                        }
                    }

                    ContentType.LIVE -> Unit
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failures += failure(account.id, type)
            }
        }

        return XtreamIndexSyncReport(
            accountCount = accounts.size,
            indexedMovieCount = movieCount,
            indexedSeriesCount = seriesCount,
            successfulMovieSources = movieSources,
            successfulSeriesSources = seriesSources,
            failures = failures,
        )
    }

    suspend fun entriesForSource(
        sourceId: Long,
        type: ContentType,
    ): List<XtreamContentIndexEntity> {
        if (type == ContentType.LIVE) return emptyList()
        return indexDao.getForSource(sourceId, type.name)
    }

    suspend fun findCandidates(
        title: String,
        type: ContentType,
        year: Int? = null,
    ): List<XtreamContentIndexEntity> {
        if (type == ContentType.LIVE) return emptyList()

        val normalized = XtreamTitleNormalizer.normalize(title)
        if (normalized.isBlank()) return emptyList()

        val results = linkedMapOf<Triple<Long, String, String>, XtreamContentIndexEntity>()

        fun add(items: List<XtreamContentIndexEntity>) {
            items.forEach { item ->
                results[Triple(item.sourceId, item.contentType, item.streamId)] = item
            }
        }

        add(
            indexDao.findCandidates(
                contentType = type.name,
                normalizedTitle = normalized,
                year = year,
            )
        )

        // Search by informative title tokens as well. This finds provider
        // titles with language/quality prefixes and extra release metadata.
        val ignored = setOf("the", "and", "for", "with", "from", "der", "die", "das")
        val tokens = normalized.split(' ')
            .filter { it.length >= 3 && it !in ignored }
            .distinct()
            .sortedByDescending { it.length }
            .take(4)

        for (token in tokens) {
            add(
                indexDao.findCandidatesContaining(
                    contentType = type.name,
                    token = token,
                    year = year,
                )
            )
        }

        return results.values.toList()
    }

    suspend fun findCandidatesByExternalIds(
        tmdbId: Int?,
        imdbId: String?,
        type: ContentType,
    ): List<XtreamContentIndexEntity> {
        if (type == ContentType.LIVE) return emptyList()
        val results = linkedMapOf<Triple<Long, String, String>, XtreamContentIndexEntity>()

        if (tmdbId != null && tmdbId > 0) {
            indexDao.findCandidatesByTmdbId(type.name, tmdbId).forEach { item ->
                results[Triple(item.sourceId, item.contentType, item.streamId)] = item
            }
        }

        val cleanImdbId = imdbId?.trim()?.lowercase()
            ?.takeIf { Regex("^tt[0-9]{5,12}$").matches(it) }
        if (cleanImdbId != null) {
            indexDao.findCandidatesByImdbId(type.name, cleanImdbId).forEach { item ->
                results[Triple(item.sourceId, item.contentType, item.streamId)] = item
            }
        }

        return results.values.toList()
    }

    suspend fun syncState(
        sourceId: Long,
        type: ContentType,
    ): CatalogSyncStateEntity? {
        if (type == ContentType.LIVE) return null
        return indexDao.getSyncState(sourceId, type.name)
    }

    private fun failure(
        sourceId: Long,
        type: ContentType,
    ) = XtreamIndexSyncFailure(
        sourceId = sourceId,
        contentType = type,
        message = "Index synchronization failed",
    )

    private fun emptyReport() = XtreamIndexSyncReport(
        accountCount = 0,
        indexedMovieCount = 0,
        indexedSeriesCount = 0,
        successfulMovieSources = 0,
        successfulSeriesSources = 0,
        failures = emptyList(),
    )

    private suspend fun <T> request(
        block: suspend () -> Result<T>,
    ): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        private const val INDEX_TTL_MS = 6 * 60 * 60 * 1000L
    }
}
