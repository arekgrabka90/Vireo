package app.vireo.data

import android.content.Context
import app.vireo.data.local.ProfileDao
import app.vireo.data.local.ProfileEntity
import app.vireo.data.security.CryptoManager
import app.vireo.data.security.PinAttemptPolicy
import app.vireo.data.security.PinCheckResult
import app.vireo.data.security.ProfilePinValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val pinMutex = Mutex()

    private fun pinFailuresKey(profileId: Long) = "pin_failures_$profileId"

    private fun pinLastFailureKey(profileId: Long) = "pin_last_failure_$profileId"

    private fun clearPinAttempts(profileId: Long) {
        prefs.edit()
            .remove(pinFailuresKey(profileId))
            .remove(pinLastFailureKey(profileId))
            .apply()
    }

    private fun currentLockRemainingMs(profileId: Long, nowMs: Long): Long {
        val failures = prefs.getInt(pinFailuresKey(profileId), 0)
        var lastFailureAt = prefs.getLong(pinLastFailureKey(profileId), 0L)
        if (failures > 0 && nowMs < lastFailureAt) {
            // Zegar urządzenia został cofnięty: odliczanie startuje od teraz.
            lastFailureAt = nowMs
            prefs.edit().putLong(pinLastFailureKey(profileId), nowMs).apply()
        }
        return PinAttemptPolicy.remainingLockMs(failures, lastFailureAt, nowMs)
    }

    /** Pozostały czas blokady weryfikacji PIN-u profilu (0, gdy brak blokady). */
    suspend fun pinLockRemainingMs(
        profileId: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Long = pinMutex.withLock { currentLockRemainingMs(profileId, nowMs) }

    suspend fun verifyPin(
        profile: ProfileEntity,
        entered: String,
    ): Boolean = checkPin(profile, entered) is PinCheckResult.Success

    /**
     * Weryfikuje PIN z limitem prób (patrz [PinAttemptPolicy]). Niepoprawny
     * format (nie cztery cyfry) nie jest liczony jako porażka.
     */
    suspend fun checkPin(
        profile: ProfileEntity,
        entered: String,
        nowMs: Long = System.currentTimeMillis(),
    ): PinCheckResult = pinMutex.withLock {
        val remaining = currentLockRemainingMs(profile.id, nowMs)
        if (remaining > 0L) return@withLock PinCheckResult.Locked(remaining)

        val failures = prefs.getInt(pinFailuresKey(profile.id), 0)
        if (!ProfilePinValidator.isFourDigitPin(entered)) {
            return@withLock PinCheckResult.Wrong(
                PinAttemptPolicy.attemptsLeftBeforeLock(failures)
            )
        }

        val valid = try {
            crypto.verifyPin(entered, profile.pin)
        } catch (_: Exception) {
            false
        }

        if (valid) {
            clearPinAttempts(profile.id)
            return@withLock PinCheckResult.Success
        }

        val newFailures = failures + 1
        prefs.edit()
            .putInt(pinFailuresKey(profile.id), newFailures)
            .putLong(pinLastFailureKey(profile.id), nowMs)
            .apply()

        val lockMs = PinAttemptPolicy.remainingLockMs(newFailures, nowMs, nowMs)
        if (lockMs > 0L) {
            PinCheckResult.Locked(lockMs)
        } else {
            PinCheckResult.Wrong(PinAttemptPolicy.attemptsLeftBeforeLock(newFailures))
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
            dao.firstExcept(profile.id)
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

        dao.deleteWithData(profile)
        clearPinAttempts(profile.id)
        profileSettingsRepository.removeProfileCache(profile.id)

        if (deletingActive) {
            activeId = fallback?.id ?: 0L
        }
    }
}
