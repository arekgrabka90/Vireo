package app.vireo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
) {
    val canSubmit: Boolean
        get() = server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !loading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (repo.hasAccount()) _state.update { st -> st.copy(loggedIn = true) }
        }
    }

    fun onServer(v: String) = _state.update { st -> st.copy(server = v, error = null) }
    fun onUsername(v: String) = _state.update { st -> st.copy(username = v, error = null) }
    fun onPassword(v: String) = _state.update { st -> st.copy(password = v, error = null) }

    fun login() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { st -> st.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.login(s.server, s.username, s.password)
                .onSuccess { _state.update { st -> st.copy(loading = false, loggedIn = true) } }
                .onFailure { e ->
                    _state.update { st -> st.copy(loading = false, error = e.message ?: "Error") }
                }
        }
    }
}
