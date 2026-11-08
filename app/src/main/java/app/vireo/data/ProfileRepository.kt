package app.vireo.data

import android.content.Context
import app.vireo.data.local.ProfileDao
import app.vireo.data.local.ProfileEntity
import app.vireo.data.security.CryptoManager
import app.vireo.data.security.ProfilePinValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val crypto: CryptoManager,
    @ApplicationContext context: Context,
) {
    private val prefs =
        context.getSharedPreferences("vireo_prefs", Context.MODE_PRIVATE)

    val profiles: Flow<List<ProfileEntity>> = dao.observeAll()

    private val _activeId = MutableStateFlow(
        prefs.getLong("active_profile", 0L)
    )
    val activeIdFlow: StateFlow<Long> = _activeId.asStateFlow()

    var activeId: Long
        get() = _activeId.value
        set(value) {
            prefs.edit().putLong("active_profile", value).apply()
            _activeId.value = value
        }

    suspend fun count(): Int = dao.count()

    suspend fun get(id: Long): ProfileEntity? = dao.get(id)

    suspend fun activeProfile(): ProfileEntity? {
        val selected = dao.get(activeId)
        if (selected != null) return selected

        val fallback = dao.first()
        activeId = fallback?.id ?: 0L
        return fallback
    }

    suspend fun verifyPin(
        profile: ProfileEntity,
        entered: String,
    ): Boolean {
        if (!ProfilePinValidator.isFourDigitPin(entered)) return false

        return try {
            crypto.verifyPin(entered, profile.pin)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun save(profile: ProfileEntity): Long {
        require(profile.name.isNotBlank()) {
            "Profile name cannot be blank"
        }
        require(ProfilePinValidator.isValid(profile.pin)) {
            "PIN must contain exactly four digits"
        }

        return if (profile.id == 0L) {
            val rawPin = profile.pin?.takeIf { it.isNotEmpty() }
            val hashed = rawPin?.let { crypto.hashPin(it) }
            dao.insert(profile.copy(pin = hashed))
        } else {
            val existing = requireNotNull(dao.get(profile.id)) {
                "Profile does not exist"
            }

            val newPin = if (profile.pin.isNullOrEmpty()) {
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

        if (activeId == profile.id) {
            val fallback = dao.first()
            activeId = fallback?.id ?: 0L
        }
    }
}
