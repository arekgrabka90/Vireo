package app.vireo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import app.vireo.data.local.AccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val needsPassword: Boolean = false,
    val reauthPassword: String = "",
    val reauthLoading: Boolean = false,
    val reauthError: String? = null,
) {
    val canSubmit: Boolean
        get() = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !loading

    val canReauthenticate: Boolean
        get() = needsPassword && reauthPassword.isNotBlank() && !reauthLoading
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
            try {
                val account = repo.activeAccount()
                val needsPassword =
                    account?.let { repo.accountNeedsPassword(it.id) } ?: false
                _state.update { st ->
                    st.copy(active = account, needsPassword = needsPassword, error = null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { st ->
                    st.copy(error = "Nie udało się odświeżyć danych źródła.")
                }
            }
        }
    }

    fun onServer(v: String) = _state.update { st -> st.copy(server = v, error = null, connected = false) }
    fun onUsername(v: String) = _state.update { st -> st.copy(username = v, error = null, connected = false) }
    fun onPassword(v: String) = _state.update { st -> st.copy(password = v, error = null, connected = false) }

    fun onReauthPassword(v: String) =
        _state.update { st -> st.copy(reauthPassword = v, reauthError = null) }

    fun reauthenticate() {
        val s = _state.value
        val account = s.active ?: return
        if (!s.canReauthenticate) return
        _state.update { st -> st.copy(reauthLoading = true, reauthError = null) }
        viewModelScope.launch {
            repo.reauthenticate(account.id, s.reauthPassword)
                .onSuccess {
                    _state.update { st ->
                        st.copy(reauthLoading = false, reauthPassword = "", needsPassword = false)
                    }
                    refresh()
                }
                .onFailure { e ->
                    _state.update { st ->
                        st.copy(reauthLoading = false, reauthError = e.message ?: "Error")
                    }
                }
        }
    }

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
