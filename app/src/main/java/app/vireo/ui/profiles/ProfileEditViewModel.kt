package app.vireo.ui.profiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.local.ProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val isFirst: Boolean = false,
    val name: String = "",
    val color: Int = 0,
    val kids: Boolean = false,
    val pin: String = "",
    val canDelete: Boolean = false,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProfileRepository,
) : ViewModel() {

    private val id: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val count = repo.count()
            val existing = if (id != 0L) repo.get(id) else null
            _state.value = if (existing != null) {
                ProfileEditState(
                    loaded = true,
                    isNew = false,
                    name = existing.name,
                    color = existing.color,
                    kids = existing.kids,
                    pin = existing.pin.orEmpty(),
                    canDelete = count > 1,
                )
            } else {
                ProfileEditState(loaded = true, isNew = true, isFirst = count == 0, color = count % 8)
            }
        }
    }

    fun onName(v: String) = _state.update { st -> st.copy(name = v.take(20)) }
    fun onColor(i: Int) = _state.update { st -> st.copy(color = i) }
    fun onKids(b: Boolean) = _state.update { st -> st.copy(kids = b) }
    fun onPin(v: String) = _state.update { st -> st.copy(pin = v.filter { ch -> ch.isDigit() }.take(4)) }

    fun save(onDone: () -> Unit) {
        val st = _state.value
        if (st.name.isBlank()) return
        viewModelScope.launch {
            val entity = ProfileEntity(
                id = id,
                name = st.name.trim(),
                color = st.color,
                kids = st.kids,
                pin = st.pin.takeIf { p -> p.length == 4 },
            )
            val savedId = repo.save(entity)
            if (repo.activeId == 0L) repo.activeId = savedId
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.get(id)?.let { p -> repo.delete(p) }
            onDone()
        }
    }
}
