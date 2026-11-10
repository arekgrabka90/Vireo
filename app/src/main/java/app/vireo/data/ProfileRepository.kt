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
    private val profileSettingsRepository: ProfileSettingsRepository,
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

    /**
     * Prepare the selected profile's persisted settings before publishing
     * its ID. UI navigation should happen only after this method succeeds.
     */
    suspend fun activateProfile(profileId: Long): ProfileEntity {
        require(profileId > 0L) { "Profile ID must be positive" }

        val profile = requireNotNull(dao.get(profileId)) {
            "Profile does not exist"
        }

        profileSettingsRepository.ensure(
            profileId = profile.id,
            kidsMode = profile.kids,
        )
        activeId = profile.id
        return profile
    }

    suspend fun activeProfile(): ProfileEntity? {
        val selected = dao.get(activeId)
        if (selected != null) {
            profileSettingsRepository.ensure(
                profileId = selected.id,
                kidsMode = selected.kids,
            )
            return selected
        }

        val fallback = dao.first()
        if (fallback == null) {
            activeId = 0L
            return null
        }

        return activateProfile(fallback.id)
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

        val profileId = if (profile.id == 0L) {
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

        profileSettingsRepository.ensure(
            profileId = profileId,
            kidsMode = profile.kids,
        )

        return profileId
    }

    suspend fun delete(profile: ProfileEntity) {
        val deletingActive = activeId == profile.id
        val fallback = if (deletingActive) {
            dao.first()?.takeIf { it.id != profile.id }
        } else {
            null
        }

        // Ensure the replacement settings before switching the active ID.
        if (fallback != null) {
            profileSettingsRepository.ensure(
                profileId = fallback.id,
                kidsMode = fallback.kids,
            )
        }

        dao.delete(profile)
        profileSettingsRepository.removeProfileCache(profile.id)

        if (deletingActive) {
            activeId = fallback?.id ?: 0L
        }
    }
}
