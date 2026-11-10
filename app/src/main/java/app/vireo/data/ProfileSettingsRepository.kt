package app.vireo.data

import app.vireo.data.local.AppSettingsDao
import app.vireo.data.local.AppSettingsEntity
import app.vireo.data.local.ProfileSettingsDao
import app.vireo.data.local.ProfileSettingsEntity
import app.vireo.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSettingsRepository @Inject constructor(
    private val profileSettingsDao: ProfileSettingsDao,
    private val appSettingsDao: AppSettingsDao,
    private val factory: ProfileSettingsFactory,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val cache =
        ConcurrentHashMap<Long, MutableStateFlow<ProfileSettingsEntity>>()
    private val profileLocks = ConcurrentHashMap<Long, Mutex>()
    private val observers = mutableMapOf<Long, Job>()

    private val _appSettings = MutableStateFlow(AppSettingsEntity())
    val appSettings: StateFlow<AppSettingsEntity> = _appSettings.asStateFlow()
    private val appLock = Mutex()
    private var appObserver: Job? = null

    private fun lockFor(profileId: Long): Mutex =
        profileLocks.getOrPut(profileId) { Mutex() }

    /**
     * Returns a safe in-memory default immediately to synchronous readers.
     * This value is not persisted unless ensure() explicitly finds no row.
     */
    fun observe(profileId: Long): StateFlow<ProfileSettingsEntity> {
        require(profileId > 0L) { "Profile ID must be positive" }
        return cache.getOrPut(profileId) {
            MutableStateFlow(factory.create(profileId))
        }.asStateFlow()
    }

    /** Synchronous snapshot for playback/HUD; never queries SQLite. */
    fun snapshot(profileId: Long): ProfileSettingsEntity {
        require(profileId > 0L) { "Profile ID must be positive" }
        return cache.getOrPut(profileId) {
            MutableStateFlow(factory.create(profileId))
        }.value
    }

    suspend fun ensure(
        profileId: Long,
        kidsMode: Boolean = false,
    ): ProfileSettingsEntity {
        require(profileId > 0L) { "Profile ID must be positive" }

        return lockFor(profileId).withLock {
            val existing = profileSettingsDao.get(profileId)
            val persisted = if (existing != null) {
                existing
            } else {
                val defaults = factory.create(profileId, kidsMode)
                profileSettingsDao.insertIfAbsent(defaults)
                profileSettingsDao.get(profileId) ?: defaults
            }

            publish(persisted)
            startObserver(profileId)
            persisted
        }
    }

    suspend fun save(settings: ProfileSettingsEntity) {
        require(settings.profileId > 0L) {
            "Profile ID must be positive"
        }

        val normalized = settings.copy(
            addonConfigsJson = ProfileSettingsJson.normalize(
                settings.addonConfigsJson
            ),
            subtitleSizeSp = settings.subtitleSizeSp.coerceIn(12, 48),
            epgRefreshIntervalMinutes =
                settings.epgRefreshIntervalMinutes.coerceIn(15, 1440),
        )

        lockFor(normalized.profileId).withLock {
            profileSettingsDao.upsert(normalized)
            publish(normalized)
            startObserver(normalized.profileId)
        }
    }

    /** Resets only the requested profile; other profiles are untouched. */
    suspend fun resetProfileSettings(
        profileId: Long,
        kidsMode: Boolean = false,
    ): ProfileSettingsEntity {
        require(profileId > 0L) { "Profile ID must be positive" }

        return lockFor(profileId).withLock {
            val defaults = factory.create(profileId, kidsMode)
            profileSettingsDao.upsert(defaults)
            publish(defaults)
            startObserver(profileId)
            defaults
        }
    }

    suspend fun getAppSettings(): AppSettingsEntity =
        appLock.withLock {
            val current = appSettingsDao.get()
            val persisted = if (current != null) {
                current
            } else {
                appSettingsDao.insertIfAbsent(AppSettingsEntity())
                appSettingsDao.get() ?: AppSettingsEntity()
            }
            _appSettings.value = persisted
            startAppObserver()
            persisted
        }

    suspend fun saveAppSettings(settings: AppSettingsEntity) {
        val normalized = settings.copy(
            id = 1,
            appLanguageTag = settings.appLanguageTag
                .takeIf { it.isNotBlank() }
                ?: "system",
        )

        appLock.withLock {
            appSettingsDao.upsert(normalized)
            _appSettings.value = normalized
            startAppObserver()
        }
    }

    /** Resets only the singleton global app-settings row. */
    suspend fun resetAppSettings(): AppSettingsEntity =
        appLock.withLock {
            val defaults = AppSettingsEntity()
            appSettingsDao.upsert(defaults)
            _appSettings.value = defaults
            startAppObserver()
            defaults
        }

    /** Called after a profile is deleted; Room also removes its row via FK. */
    fun removeProfileCache(profileId: Long) {
        synchronized(observers) {
            observers.remove(profileId)?.cancel()
        }
        cache.remove(profileId)
        profileLocks.remove(profileId)
    }

    private fun publish(settings: ProfileSettingsEntity) {
        cache.getOrPut(settings.profileId) {
            MutableStateFlow(settings)
        }.value = settings
    }

    private fun startObserver(profileId: Long) {
        synchronized(observers) {
            if (observers[profileId]?.isActive == true) return

            observers[profileId] = applicationScope.launch {
                profileSettingsDao.observe(profileId).collect { settings ->
                    if (settings != null) publish(settings)
                }
            }
        }
    }

    private fun startAppObserver() {
        synchronized(this) {
            if (appObserver?.isActive == true) return
            appObserver = applicationScope.launch {
                appSettingsDao.observe().collect { settings ->
                    if (settings != null) _appSettings.value = settings
                }
            }
        }
    }
}
