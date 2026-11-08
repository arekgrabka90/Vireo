package app.vireo.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.local.ProfileEntity
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ProfilesState(
    val loaded: Boolean = false,
    val profiles: List<ProfileEntity> = emptyList(),
    val manage: Boolean = false,
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {

    private val manage = MutableStateFlow(false)

    val state: StateFlow<ProfilesState> = combine(repo.profiles, manage) { list, m ->
        ProfilesState(loaded = true, profiles = list, manage = m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesState())

    fun toggleManage() {
        manage.update { current -> !current }
    }

    fun select(profile: ProfileEntity) {
        repo.activeId = profile.id
    }

    fun verifyPin(
        profile: ProfileEntity,
        entered: String,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(repo.verifyPin(profile, entered))
        }
    }
}
