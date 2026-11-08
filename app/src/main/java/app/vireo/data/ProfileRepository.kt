package app.vireo.data

import android.content.Context
import app.vireo.data.local.ProfileDao
import app.vireo.data.local.ProfileEntity
import app.vireo.data.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val crypto: CryptoManager,
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

    suspend fun verifyPin(profile: ProfileEntity, entered: String): Boolean =
        crypto.verifyPin(entered, profile.pin)

    suspend fun save(profile: ProfileEntity): Long {
        return if (profile.id == 0L) {
            val rawPin = profile.pin
            val hashed = if (rawPin.isNullOrBlank()) null else crypto.hashPin(rawPin)
            dao.insert(profile.copy(pin = hashed))
        } else {
            val existing = requireNotNull(dao.get(profile.id)) {
                "Profile does not exist"
            }
            val newPin = if (profile.pin.isNullOrBlank()) {
                existing.pin
            } else {
                crypto.hashPin(profile.pin)
            }
            dao.update(profile.copy(pin = newPin))
            profile.id
        }
    }

    suspend fun delete(profile: ProfileEntity) {
        dao.delete(profile)
        if (activeId == profile.id) activeId = 0L
    }
}
