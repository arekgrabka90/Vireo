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

    suspend fun remove(profileId: Long, contentKey: String) {
        dao.delete(profileId, contentKey)
    }
}
