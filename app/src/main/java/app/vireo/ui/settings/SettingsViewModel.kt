package app.vireo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import app.vireo.data.local.AppSettingsEntity
import app.vireo.data.local.ProfileEntity
import app.vireo.data.local.ProfileSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: ProfileSettingsRepository,
) : ViewModel() {

    val active: StateFlow<ProfileEntity?> = profiles.activeIdFlow
        .flatMapLatest { id ->
            if (id <= 0L) {
                flowOf(null)
            } else {
                kotlinx.coroutines.flow.flow {
                    emit(profiles.get(id))
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
                    settings.observe(id).map { it }
                } else {
                    flowOf(null)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null,
            )

    val appSettings: StateFlow<AppSettingsEntity> = settings.appSettings

    init {
        viewModelScope.launch {
            profiles.activeProfile()
            settings.getAppSettings()
        }
    }

    fun resetCurrentProfile() {
        val id = profiles.activeId
        if (id <= 0L) return

        viewModelScope.launch {
            val profile = profiles.get(id) ?: return@launch
            settings.resetProfileSettings(id, profile.kids)
        }
    }

    fun resetAppSettings() {
        viewModelScope.launch {
            settings.resetAppSettings()
        }
    }
}
