package app.vireo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import app.vireo.data.local.AppSettingsEntity
import app.vireo.data.local.ProfileEntity
import app.vireo.data.local.ProfileSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsOperationState(
    val savingCount: Int = 0,
    val error: String? = null,
) {
    val isSaving: Boolean get() = savingCount > 0
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: ProfileSettingsRepository,
) : ViewModel() {

    val active: StateFlow<ProfileEntity?> = profiles.activeIdFlow
        .flatMapLatest { id ->
            if (id <= 0L) {
                flowOf<ProfileEntity?>(null)
            } else {
                profiles.profiles.map { list ->
                    list.firstOrNull { it.id == id }
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    val profileSettings: StateFlow<ProfileSettingsEntity?> =
        profiles.activeIdFlow
            .flatMapLatest { id ->
                if (id > 0L) {
                    settings.observe(id).map<ProfileSettingsEntity, ProfileSettingsEntity?> {
                        it
                    }
                } else {
                    flowOf<ProfileSettingsEntity?>(null)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null,
            )

    val appSettings: StateFlow<AppSettingsEntity> = settings.appSettings

    private val _operationState = MutableStateFlow(SettingsOperationState())
    val operationState: StateFlow<SettingsOperationState> =
        _operationState.asStateFlow()

    private var runningOperations = 0

    init {
        launchOperation {
            profiles.activeProfile()
            settings.getAppSettings()
        }
    }

    fun updateProfile(
        transform: (ProfileSettingsEntity) -> ProfileSettingsEntity,
    ) {
        val profileId = profiles.activeId
        if (profileId <= 0L) return

        launchOperation {
            val profile = requireNotNull(profiles.get(profileId)) {
                "The selected profile no longer exists."
            }
            settings.updateProfileSettings(
                profileId = profileId,
                kidsMode = profile.kids,
                transform = transform,
            )
        }
    }

    fun updateApp(
        transform: (AppSettingsEntity) -> AppSettingsEntity,
    ) {
        launchOperation {
            settings.updateAppSettings(transform)
        }
    }

    fun resetCurrentProfile() {
        val profileId = profiles.activeId
        if (profileId <= 0L) return

        launchOperation {
            val profile = requireNotNull(profiles.get(profileId)) {
                "The selected profile no longer exists."
            }
            settings.resetProfileSettings(profileId, profile.kids)
        }
    }

    fun resetAppSettings() {
        launchOperation {
            settings.resetAppSettings()
        }
    }

    private fun launchOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            runningOperations += 1
            _operationState.value = _operationState.value.copy(
                savingCount = runningOperations,
                error = null,
            )

            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operationState.value = _operationState.value.copy(
                    error = error.message?.takeIf { it.isNotBlank() }
                        ?: "Could not save settings. Please try again.",
                )
            } finally {
                runningOperations = (runningOperations - 1).coerceAtLeast(0)
                _operationState.value = _operationState.value.copy(
                    savingCount = runningOperations,
                )
            }
        }
    }
}
