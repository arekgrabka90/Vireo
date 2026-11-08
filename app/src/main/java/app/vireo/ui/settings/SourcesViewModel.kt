package app.vireo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import app.vireo.data.local.AccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourcesState(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val connected: Boolean = false,
    val active: AccountEntity? = null,
) {
    val canSubmit: Boolean
        get() = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !loading
}

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SourcesState())
    val state: StateFlow<SourcesState> = _state.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            val account = repo.activeAccount()
            _state.update { st -> st.copy(active = account) }
        }
    }

    fun onServer(v: String) = _state.update { st -> st.copy(server = v, error = null, connected = false) }
    fun onUsername(v: String) = _state.update { st -> st.copy(username = v, error = null, connected = false) }
    fun onPassword(v: String) = _state.update { st -> st.copy(password = v, error = null, connected = false) }

    fun save() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { st -> st.copy(loading = true, error = null, connected = false) }
        viewModelScope.launch {
            repo.login(s.server, s.username, s.password)
                .onSuccess {
                    _state.update { st ->
                        st.copy(loading = false, connected = true, server = "", username = "", password = "")
                    }
                    refresh()
                }
                .onFailure { e ->
                    _state.update { st -> st.copy(loading = false, error = e.message ?: "Error") }
                }
        }
    }

    fun remove() {
        viewModelScope.launch {
            repo.logout()
            _state.update { st -> st.copy(active = null, connected = false) }
        }
    }
}
