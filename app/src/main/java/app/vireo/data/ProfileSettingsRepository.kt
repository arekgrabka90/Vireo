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

    private val observers = mutableMapOf<Long, Job>()

    suspend fun ensure(
        profileId: Long,
        kidsMode: Boolean = false,
    ): ProfileSettingsEntity {
        require(profileId > 0L) { "Profile ID must be positive" }

        val existing = profileSettingsDao.get(profileId)
        if (existing != null) {
            publish(existing)
            startObserver(profileId)
            return existing
        }

        val defaults = factory.create(profileId, kidsMode)
        profileSettingsDao.insertIfAbsent(defaults)

        val persisted = profileSettingsDao.get(profileId) ?: defaults
        publish(persisted)
        startObserver(profileId)
        return persisted
    }

    fun observe(profileId: Long): StateFlow<ProfileSettingsEntity> {
        require(profileId > 0L) { "Profile ID must be positive" }

        return cache.getOrPut(profileId) {
            MutableStateFlow(factory.create(profileId))
        }.asStateFlow()
    }

    /**
     * Synchronous in-memory snapshot for playback/HUD code.
     * It never performs a SQLite query.
     */
    fun snapshot(profileId: Long): ProfileSettingsEntity {
        require(profileId > 0L) { "Profile ID must be positive" }

        return cache.getOrPut(profileId) {
            MutableStateFlow(factory.create(profileId))
        }.value
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

        profileSettingsDao.upsert(normalized)
        publish(normalized)
        startObserver(normalized.profileId)
    }

    suspend fun getAppSettings(): AppSettingsEntity {
        val current = appSettingsDao.get()
        if (current != null) return current

        appSettingsDao.insertIfAbsent(AppSettingsEntity())
        return appSettingsDao.get() ?: AppSettingsEntity()
    }

    suspend fun saveAppSettings(settings: AppSettingsEntity) {
        appSettingsDao.upsert(
            settings.copy(
                id = 1,
                appLanguageTag = settings.appLanguageTag
                    .takeIf { it.isNotBlank() }
                    ?: "system",
            )
        )
    }

    private fun publish(settings: ProfileSettingsEntity) {
        cache.getOrPut(settings.profileId) {
            MutableStateFlow(settings)
        }.value = settings
    }

    private fun startObserver(profileId: Long) {
        synchronized(observers) {
            if (observers.containsKey(profileId)) return

            observers[profileId] = applicationScope.launch {
                profileSettingsDao.observe(profileId).collect { settings ->
                    if (settings != null) publish(settings)
                }
            }
        }
    }
}
