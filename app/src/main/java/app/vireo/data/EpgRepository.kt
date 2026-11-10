package app.vireo.data

import app.vireo.data.local.AccountDao
import app.vireo.data.local.EpgDao
import app.vireo.data.local.EpgProgramEntity
import app.vireo.data.ProviderRegistry
import app.vireo.domain.model.EpgNowNext
import app.vireo.domain.model.EpgProgram
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal object EpgCachePolicy {
    const val DEFAULT_REFRESH_TTL_MS = 15 * 60 * 1000L
    fun refreshTtlMs(minutes: Int): Long =
        minutes.coerceIn(15, 1440) * 60_000L
    const val LOOKBACK_MS = 6 * 60 * 60 * 1000L
    const val LOOKAHEAD_MS = 48 * 60 * 60 * 1000L
    const val RETENTION_PAST_MS = 7 * 24 * 60 * 60 * 1000L
}

@Singleton
class EpgRepository @Inject constructor(
    private val accounts: AccountDao,
    private val epgDao: EpgDao,
    private val providers: ProviderRegistry,
) {
    private val syncMutex = Mutex()

    suspend fun nowNext(
        streamId: String,
        refreshIntervalMinutes: Int = 15,
    ): Result<EpgNowNext> = try {
        withContext(Dispatchers.IO) {
            require(streamId.isNotBlank()) { "Missing stream_id" }
            val refreshTtlMs = EpgCachePolicy.refreshTtlMs(
                refreshIntervalMinutes
            )

            val account = accounts.getActive()
                ?: error("No active content source")

            val now = System.currentTimeMillis()
            val from = now - EpgCachePolicy.LOOKBACK_MS
            val to = now + EpgCachePolicy.LOOKAHEAD_MS

            var cached = epgDao.getPrograms(account.id, streamId, from, to)
            val sync = epgDao.getSyncState(account.id, streamId)
            val stale = sync == null ||
                now - sync.lastAttemptAt >= refreshTtlMs

            if (stale) {
                syncMutex.withLock {
                    if (accounts.getActive()?.id != account.id) {
                        error("Active source changed during EPG refresh")
                    }

                    val latestSync = epgDao.getSyncState(account.id, streamId)
                    val refreshTime = System.currentTimeMillis()
                    val stillStale = latestSync == null ||
                        refreshTime - latestSync.lastAttemptAt >= refreshTtlMs

                    if (stillStale) {
                        val schedule = try {
                            providers.create(account)
                                .schedule(streamId, from, to)
                                .getOrThrow()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            if (accounts.getActive()?.id != account.id) {
                                throw IllegalStateException(
                                    "Active source changed during EPG request",
                                    error,
                                )
                            }
                            epgDao.recordAttempt(
                                account.id,
                                streamId,
                                System.currentTimeMillis(),
                            )
                            null
                        }

                        if (schedule != null) {
                            val incoming = schedule.programs
                                .filter { it.hasValidTimeRange }
                                .distinctBy { it.startTimeMs to it.title }

                            if (incoming.isNotEmpty()) {
                                if (accounts.getActive()?.id != account.id) {
                                    error("Active source changed; EPG response discarded")
                                }

                                val syncedAt = System.currentTimeMillis()
                                val entities = incoming.map {
                                    EpgProgramEntity(
                                        sourceId = account.id,
                                        streamId = streamId,
                                        title = it.title,
                                        description = it.description,
                                        startTimeMs = it.startTimeMs,
                                        endTimeMs = it.endTimeMs,
                                        category = it.category,
                                        iconUrl = it.iconUrl,
                                    )
                                }
                                epgDao.replaceSchedule(
                                    account.id,
                                    streamId,
                                    entities,
                                    syncedAt,
                                )
                            } else {
                                // An empty response is not proof that the old cache is invalid.
                                epgDao.recordAttempt(
                                    account.id,
                                    streamId,
                                    System.currentTimeMillis(),
                                )
                            }
                        }
                    }
                }

                cached = epgDao.getPrograms(account.id, streamId, from, to)
            }

            epgDao.deleteExpired(now - EpgCachePolicy.RETENTION_PAST_MS)

            if (accounts.getActive()?.id != account.id) {
                error("Active source changed while loading EPG")
            }

            val programs = cached.map {
                EpgProgram(
                    channelId = streamId,
                    title = it.title,
                    description = it.description,
                    startTimeMs = it.startTimeMs,
                    endTimeMs = it.endTimeMs,
                    category = it.category,
                    iconUrl = it.iconUrl,
                )
            }

            val selectionTime = System.currentTimeMillis()
            val current = programs.firstOrNull { it.contains(selectionTime) }
            val next = programs
                .filter { it.startTimeMs > selectionTime }
                .minByOrNull { it.startTimeMs }

            Result.success(EpgNowNext(current, next))
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
