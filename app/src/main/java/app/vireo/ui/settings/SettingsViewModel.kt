package app.vireo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.local.ProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val _active = MutableStateFlow<ProfileEntity?>(null)
    val active: StateFlow<ProfileEntity?> = _active.asStateFlow()

    init {
        viewModelScope.launch { _active.value = profiles.activeProfile() }
    }
}
