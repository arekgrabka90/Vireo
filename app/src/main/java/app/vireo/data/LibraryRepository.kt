package app.vireo.data

import app.vireo.data.local.LibraryDao
import app.vireo.data.local.LibraryItemEntity
import app.vireo.player.PlayItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val dao: LibraryDao,
) {
    fun observeAll(profileId: Long): Flow<List<LibraryItemEntity>> =
        dao.observeAll(profileId)

    fun observeWatchlist(profileId: Long): Flow<List<LibraryItemEntity>> =
        dao.observeWatchlist(profileId)

    fun observeContinueWatching(profileId: Long): Flow<List<LibraryItemEntity>> =
        dao.observeContinueWatching(profileId)

    suspend fun resumePosition(
        profileId: Long,
        contentKey: String,
    ): Long {
        if (profileId <= 0L || contentKey.isBlank()) return 0L

        val item = dao.get(profileId, contentKey) ?: return 0L

        if (item.watched || item.durationMs <= 0L) return 0L

        return item.positionMs.coerceIn(0L, item.durationMs)
    }

    suspend fun saveProgress(
        profileId: Long,
        item: PlayItem,
        positionMs: Long,
        durationMs: Long,
    ) {
        val key = item.contentKey?.takeIf { it.isNotBlank() } ?: return
        if (profileId <= 0L) return

        val old = dao.get(profileId, key)
        val progress = normalizeLibraryProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            previousDurationMs = old?.durationMs ?: 0L,
            alreadyWatched = old?.watched == true,
        )

        dao.upsert(
            LibraryItemEntity(
                profileId = profileId,
                contentKey = key,
                title = item.title,
                contentType = item.contentType ?: "UNKNOWN",
                posterUrl = item.posterUrl ?: old?.posterUrl,
                backdropUrl = item.backdropUrl ?: old?.backdropUrl,
                positionMs = progress.positionMs,
                durationMs = progress.durationMs,
                watchlisted = old?.watchlisted ?: false,
                watched = progress.watched,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun isWatchlisted(profileId: Long, contentKey: String): Boolean {
        if (profileId <= 0L) return false
        return dao.get(profileId, contentKey)?.watchlisted == true
    }

    suspend fun isWatched(profileId: Long, contentKey: String): Boolean {
        if (profileId <= 0L || contentKey.isBlank()) return false
        return dao.get(profileId, contentKey)?.watched == true
    }

    suspend fun setWatchlisted(
        profileId: Long,
        contentKey: String,
        title: String,
        contentType: String,
        posterUrl: String?,
        backdropUrl: String?,
        enabled: Boolean,
    ) {
        if (profileId <= 0L || contentKey.isBlank()) return

        val old = dao.get(profileId, contentKey)
        dao.upsert(
            LibraryItemEntity(
                profileId = profileId,
                contentKey = contentKey,
                title = title,
                contentType = contentType,
                posterUrl = posterUrl ?: old?.posterUrl,
                backdropUrl = backdropUrl ?: old?.backdropUrl,
                positionMs = old?.positionMs ?: 0L,
                durationMs = old?.durationMs ?: 0L,
                watchlisted = enabled,
                watched = old?.watched ?: false,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun setWatched(
        profileId: Long,
        contentKey: String,
        watched: Boolean,
        title: String = "",
        contentType: String = "UNKNOWN",
        posterUrl: String? = null,
        backdropUrl: String? = null,
    ) {
        if (profileId <= 0L || contentKey.isBlank()) return

        val old = dao.get(profileId, contentKey)
        val now = System.currentTimeMillis()

        dao.upsert(
            old?.copy(
                watched = watched,
                positionMs = if (watched) 0L else old.positionMs,
                updatedAt = now,
            ) ?: LibraryItemEntity(
                profileId = profileId,
                contentKey = contentKey,
                title = title.ifBlank { contentKey },
                contentType = contentType,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                positionMs = 0L,
                durationMs = 0L,
                watchlisted = false,
                watched = watched,
                updatedAt = now,
            )
        )
    }

    suspend fun remove(profileId: Long, contentKey: String) {
        dao.delete(profileId, contentKey)
    }
}
