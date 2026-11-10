package app.vireo.data

import app.vireo.data.local.RecentSearchDao
import app.vireo.data.local.RecentSearchEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentSearchRepository @Inject constructor(
    private val dao: RecentSearchDao,
) {
    fun observe(profileId: Long): Flow<List<String>> =
        dao.observe(profileId).map { entries -> entries.map { it.query } }

    suspend fun add(profileId: Long, rawQuery: String) {
        if (profileId <= 0L) return
        val query = rawQuery.trim().take(MAX_QUERY_LENGTH)
        val normalized = query.lowercase(Locale.ROOT)
        if (normalized.isBlank()) return

        dao.add(
            RecentSearchEntity(
                profileId = profileId,
                normalizedQuery = normalized,
                query = query,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clear(profileId: Long) {
        if (profileId > 0L) dao.clear(profileId)
    }

    companion object {
        private const val MAX_QUERY_LENGTH = 160
    }
}
