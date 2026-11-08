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

    suspend fun saveProgress(
        profileId: Long,
        item: PlayItem,
        positionMs: Long,
        durationMs: Long,
    ) {
        val key = item.contentKey?.takeIf { it.isNotBlank() } ?: return
        if (profileId <= 0L) return

        val old = dao.get(profileId, key)
        val duration = durationMs.coerceAtLeast(0L)
        val position = positionMs.coerceAtLeast(0L)
        val completed = duration > 0L &&
            position >= (duration * 0.95).toLong()

        dao.upsert(
            LibraryItemEntity(
                profileId = profileId,
                contentKey = key,
                title = item.title,
                contentType = item.contentType ?: "UNKNOWN",
                posterUrl = item.posterUrl ?: old?.posterUrl,
                backdropUrl = item.backdropUrl ?: old?.backdropUrl,
                positionMs = if (completed) 0L else position,
                durationMs = duration,
                watchlisted = old?.watchlisted ?: false,
                watched = old?.watched == true || completed,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun isWatchlisted(profileId: Long, contentKey: String): Boolean {
        if (profileId <= 0L) return false
        return dao.get(profileId, contentKey)?.watchlisted == true
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
    ) {
        if (profileId <= 0L) return
        val old = dao.get(profileId, contentKey) ?: return
        dao.upsert(
            old.copy(
                watched = watched,
                positionMs = if (watched) 0L else old.positionMs,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun remove(profileId: Long, contentKey: String) {
        dao.delete(profileId, contentKey)
    }
}
