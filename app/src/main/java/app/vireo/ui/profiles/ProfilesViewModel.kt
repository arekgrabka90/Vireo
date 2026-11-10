package app.vireo.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.local.ProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilesState(
    val loaded: Boolean = false,
    val profiles: List<ProfileEntity> = emptyList(),
    val manage: Boolean = false,
    val isSelecting: Boolean = false,
    val selectionError: Boolean = false,
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {

    private val manage = MutableStateFlow(false)
    private val isSelecting = MutableStateFlow(false)
    private val selectionError = MutableStateFlow(false)

    val state: StateFlow<ProfilesState> = combine(
        repo.profiles,
        manage,
        isSelecting,
        selectionError,
    ) { profiles, managing, selecting, failed ->
        ProfilesState(
            loaded = true,
            profiles = profiles,
            manage = managing,
            isSelecting = selecting,
            selectionError = failed,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfilesState(),
    )

    fun toggleManage() {
        manage.update { current -> !current }
    }

    fun select(profile: ProfileEntity, onSelected: () -> Unit) {
        if (isSelecting.value) return

        isSelecting.value = true
        selectionError.value = false

        viewModelScope.launch {
            try {
                repo.activateProfile(profile.id)
                isSelecting.value = false
                onSelected()
            } catch (cancelled: CancellationException) {
                isSelecting.value = false
                throw cancelled
            } catch (_: Exception) {
                isSelecting.value = false
                selectionError.value = true
            }
        }
    }

    fun verifyPin(
        profile: ProfileEntity,
        entered: String,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val valid = try {
                repo.verifyPin(profile, entered)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }

            onResult(valid)
        }
    }
}
