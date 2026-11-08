package app.vireo.data.xtream

import app.vireo.data.ProviderRegistry
import app.vireo.data.local.AccountDao
import app.vireo.data.local.CatalogSyncStateEntity
import app.vireo.data.local.XtreamContentIndexDao
import app.vireo.data.local.XtreamContentIndexEntity
import app.vireo.domain.model.ContentType
import kotlinx.coroutines.CancellationException
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

@Singleton
class XtreamContentIndexRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val indexDao: XtreamContentIndexDao,
    private val providers: ProviderRegistry,
) {

    suspend fun syncAll(): XtreamIndexSyncReport {
        val accounts = accountDao.getAll()
        val failures = mutableListOf<XtreamIndexSyncFailure>()

        var movieCount = 0
        var seriesCount = 0
        var movieSources = 0
        var seriesSources = 0

        for (account in accounts) {
            val provider = try {
                providers.create(account)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failures += failure(account.id, ContentType.MOVIE)
                failures += failure(account.id, ContentType.SERIES)
                continue
            }

            val movies = request { provider.movies(null) }

            if (movies.isSuccess) {
                try {
                    val now = System.currentTimeMillis()
                    val entries = movies.getOrThrow().mapNotNull {
                        XtreamContentIndexMapper.movie(account.id, it, now)
                    }

                    indexDao.replaceForSourceAndType(
                        sourceId = account.id,
                        contentType = ContentType.MOVIE.name,
                        items = entries,
                        syncedAt = now,
                    )

                    movieCount += entries.size
                    movieSources++
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failures += failure(account.id, ContentType.MOVIE)
                }
            } else {
                failures += failure(account.id, ContentType.MOVIE)
            }

            val series = request { provider.series(null) }

            if (series.isSuccess) {
                try {
                    val now = System.currentTimeMillis()
                    val entries = series.getOrThrow().mapNotNull {
                        XtreamContentIndexMapper.series(account.id, it, now)
                    }

                    indexDao.replaceForSourceAndType(
                        sourceId = account.id,
                        contentType = ContentType.SERIES.name,
                        items = entries,
                        syncedAt = now,
                    )

                    seriesCount += entries.size
                    seriesSources++
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failures += failure(account.id, ContentType.SERIES)
                }
            } else {
                failures += failure(account.id, ContentType.SERIES)
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

        return indexDao.findCandidates(
            contentType = type.name,
            normalizedTitle = normalized,
            year = year,
        )
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

    private suspend fun <T> request(
        block: suspend () -> Result<T>,
    ): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
