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

/**
 * Central validation for persisted preferences.
 *
 * Values are normalized at the repository boundary so that UI controls,
 * future runtime integrations and other callers share the same constraints.
 */
object ProfileSettingsSanitizer {
    private val themes = setOf("system", "light", "dark")
    private val accents = setOf("violet", "blue", "teal", "amber", "rose")
    private val hudStyles = setOf("classic", "chips")

    private fun language(value: String, fallback: String): String {
        val candidate = value.trim()
        if (candidate == "system") return candidate
        return candidate.takeIf {
            it.length <= 35 &&
                Regex("^[A-Za-z]{2,3}([-_][A-Za-z0-9]{2,8})*$")
                    .matches(it)
        } ?: fallback
    }

    fun profile(value: ProfileSettingsEntity): ProfileSettingsEntity =
        value.copy(
            contentLanguage = language(value.contentLanguage, "en-US"),
            preferredAudioLanguage =
                language(value.preferredAudioLanguage, "system"),
            preferredSubtitleLanguage =
                language(value.preferredSubtitleLanguage, "system"),
            subtitleSizeSp = value.subtitleSizeSp.coerceIn(12, 48),
            themeMode = value.themeMode
                .takeIf { it in themes } ?: "system",
            accentColor = value.accentColor
                .takeIf { it in accents } ?: "violet",
            liveTvHudTechStyle = value.liveTvHudTechStyle
                .takeIf { it in hudStyles } ?: "classic",
            epgRefreshIntervalMinutes =
                value.epgRefreshIntervalMinutes.coerceIn(15, 1440),
            addonConfigsJson =
                ProfileSettingsJson.normalize(value.addonConfigsJson),
        )

    fun app(value: AppSettingsEntity): AppSettingsEntity =
        value.copy(
            id = 1,
            appLanguageTag = language(value.appLanguageTag, "system"),
        )
}

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
     * Synchronous safe snapshot for player/HUD consumers.
     * It never performs a database query.
     */
    fun observe(profileId: Long): StateFlow<ProfileSettingsEntity> {
        require(profileId > 0L) { "Profile ID must be positive" }
        return cache.getOrPut(profileId) {
            MutableStateFlow(factory.create(profileId))
        }.asStateFlow()
    }

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
                ProfileSettingsSanitizer.profile(existing).also {
                    if (it != existing) profileSettingsDao.upsert(it)
                }
            } else {
                val defaults = factory.create(profileId, kidsMode)
                profileSettingsDao.insertIfAbsent(defaults)
                profileSettingsDao.get(profileId)?.let {
                    ProfileSettingsSanitizer.profile(it)
                } ?: defaults
            }

            publish(persisted)
            startObserver(profileId)
            persisted
        }
    }

    /**
     * Field-level update. The transform is applied to the latest persisted
     * row while holding the profile mutex, preventing stale UI snapshots
     * from overwriting unrelated fields during rapid changes.
     */
    suspend fun updateProfileSettings(
        profileId: Long,
        kidsMode: Boolean = false,
        transform: (ProfileSettingsEntity) -> ProfileSettingsEntity,
    ): ProfileSettingsEntity {
        require(profileId > 0L) { "Profile ID must be positive" }

        return lockFor(profileId).withLock {
            val latest = profileSettingsDao.get(profileId)
                ?: factory.create(profileId, kidsMode)

            val updated = ProfileSettingsSanitizer.profile(
                transform(latest).also {
                    require(it.profileId == profileId) {
                        "A profile settings update cannot change profileId"
                    }
                }
            )

            profileSettingsDao.upsert(updated)
            publish(updated)
            startObserver(profileId)
            updated
        }
    }

    suspend fun save(settings: ProfileSettingsEntity) {
        require(settings.profileId > 0L) {
            "Profile ID must be positive"
        }

        val normalized = ProfileSettingsSanitizer.profile(settings)

        lockFor(normalized.profileId).withLock {
            profileSettingsDao.upsert(normalized)
            publish(normalized)
            startObserver(normalized.profileId)
        }
    }

    suspend fun resetProfileSettings(
        profileId: Long,
        kidsMode: Boolean = false,
    ): ProfileSettingsEntity {
        require(profileId > 0L) { "Profile ID must be positive" }

        return lockFor(profileId).withLock {
            val defaults = ProfileSettingsSanitizer.profile(
                factory.create(profileId, kidsMode)
            )
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
                ProfileSettingsSanitizer.app(current).also {
                    if (it != current) appSettingsDao.upsert(it)
                }
            } else {
                appSettingsDao.insertIfAbsent(AppSettingsEntity())
                appSettingsDao.get()?.let(ProfileSettingsSanitizer::app)
                    ?: AppSettingsEntity()
            }
            _appSettings.value = persisted
            startAppObserver()
            persisted
        }

    suspend fun updateAppSettings(
        transform: (AppSettingsEntity) -> AppSettingsEntity,
    ): AppSettingsEntity = appLock.withLock {
        val latest = appSettingsDao.get() ?: AppSettingsEntity()
        val updated = ProfileSettingsSanitizer.app(transform(latest))
        appSettingsDao.upsert(updated)
        _appSettings.value = updated
        startAppObserver()
        updated
    }

    suspend fun saveAppSettings(settings: AppSettingsEntity) {
        val normalized = ProfileSettingsSanitizer.app(settings)

        appLock.withLock {
            appSettingsDao.upsert(normalized)
            _appSettings.value = normalized
            startAppObserver()
        }
    }

    suspend fun resetAppSettings(): AppSettingsEntity =
        appLock.withLock {
            val defaults = AppSettingsEntity()
            appSettingsDao.upsert(defaults)
            _appSettings.value = defaults
            startAppObserver()
            defaults
        }

    /** Room deletes the row through its foreign key when a profile is removed. */
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
                    if (settings != null) {
                        publish(ProfileSettingsSanitizer.profile(settings))
                    }
                }
            }
        }
    }

    private fun startAppObserver() {
        synchronized(this) {
            if (appObserver?.isActive == true) return
            appObserver = applicationScope.launch {
                appSettingsDao.observe().collect { settings ->
                    if (settings != null) {
                        _appSettings.value = ProfileSettingsSanitizer.app(settings)
                    }
                }
            }
        }
    }
}
