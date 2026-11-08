package app.vireo.ui.profiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.R
import app.vireo.data.ProfileRepository
import app.vireo.data.local.ProfileEntity
import app.vireo.data.security.ProfilePinValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditState(
    val loaded: Boolean = false,
    val loadFailed: Boolean = false,
    val isNew: Boolean = true,
    val isFirst: Boolean = false,
    val name: String = "",
    val color: Int = 0,
    val kids: Boolean = false,
    val pin: String = "",
    val canDelete: Boolean = false,
    val isSaving: Boolean = false,
    val errorRes: Int? = null,
) {
    val pinValid: Boolean
        get() = ProfilePinValidator.isValid(pin)
}

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProfileRepository,
) : ViewModel() {

    private val id: Long =
        savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val count = repo.count()
                val existing = if (id != 0L) repo.get(id) else null

                _state.value = if (id != 0L && existing == null) {
                    ProfileEditState(
                        loaded = true,
                        isNew = false,
                        loadFailed = true,
                        errorRes = R.string.profile_load_error,
                    )
                } else if (existing != null) {
                    ProfileEditState(
                        loaded = true,
                        isNew = false,
                        name = existing.name,
                        color = existing.color,
                        kids = existing.kids,
                        pin = "",
                        canDelete = count > 1,
                    )
                } else {
                    ProfileEditState(
                        loaded = true,
                        isNew = true,
                        isFirst = count == 0,
                        color = count % 8,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = ProfileEditState(
                    loaded = true,
                    loadFailed = true,
                    errorRes = R.string.profile_load_error,
                )
            }
        }
    }

    fun onName(value: String) =
        _state.update { it.copy(name = value.take(20), errorRes = null) }

    fun onColor(value: Int) =
        _state.update { it.copy(color = value, errorRes = null) }

    fun onKids(value: Boolean) =
        _state.update { it.copy(kids = value, errorRes = null) }

    fun onPin(value: String) =
        _state.update {
            it.copy(
                pin = value.filter { char -> char in '0'..'9' }.take(4),
                errorRes = null,
            )
        }

    fun save(onDone: () -> Unit) {
        val current = _state.value
        if (
            !current.loaded ||
            current.loadFailed ||
            current.isSaving ||
            current.name.isBlank() ||
            !current.pinValid
        ) return

        _state.update {
            it.copy(isSaving = true, errorRes = null)
        }

        viewModelScope.launch {
            val entity = ProfileEntity(
                id = id,
                name = current.name.trim(),
                color = current.color,
                kids = current.kids,
                pin = current.pin.takeIf { it.isNotEmpty() },
            )

            try {
                repo.save(entity)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorRes = R.string.profile_save_error,
                    )
                }
                return@launch
            }

            try {
                repo.activeProfile()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Zapis profilu już się powiódł.
            }

            _state.update { it.copy(isSaving = false) }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val current = _state.value
        if (!current.loaded || current.loadFailed || id == 0L || current.isSaving) return

        _state.update {
            it.copy(isSaving = true, errorRes = null)
        }

        viewModelScope.launch {
            try {
                repo.get(id)?.let { profile -> repo.delete(profile) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorRes = R.string.profile_delete_error,
                    )
                }
                return@launch
            }

            _state.update { it.copy(isSaving = false) }
            onDone()
        }
    }
}
