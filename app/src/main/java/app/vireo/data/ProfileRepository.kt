package app.vireo.data

import android.content.Context
import app.vireo.data.local.ProfileDao
import app.vireo.data.local.ProfileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("vireo_prefs", Context.MODE_PRIVATE)

    val profiles: Flow<List<ProfileEntity>> = dao.observeAll()

    var activeId: Long
        get() = prefs.getLong("active_profile", 0L)
        set(value) {
            prefs.edit().putLong("active_profile", value).apply()
        }

    suspend fun count(): Int = dao.count()

    suspend fun get(id: Long): ProfileEntity? = dao.get(id)

    suspend fun activeProfile(): ProfileEntity? = dao.get(activeId)

    suspend fun save(profile: ProfileEntity): Long =
        if (profile.id == 0L) {
            dao.insert(profile)
        } else {
            dao.update(profile)
            profile.id
        }

    suspend fun delete(profile: ProfileEntity) {
        dao.delete(profile)
        if (activeId == profile.id) activeId = 0L
    }
}
