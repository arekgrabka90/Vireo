package app.vireo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val tab: ContentType = ContentType.MOVIE,
    val categories: Map<ContentType, List<Category>> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { st -> st.copy(loading = true, error = null) }
            val provider = repo.provider()
            if (provider == null) {
                _state.update { st -> st.copy(loading = false, error = "No account") }
                return@launch
            }
            runCatching {
                coroutineScope {
                    ContentType.entries
                        .map { type -> async { type to provider.categories(type).getOrThrow() } }
                        .awaitAll()
                        .toMap()
                }
            }.onSuccess { map ->
                _state.update { st -> st.copy(loading = false, categories = map) }
            }.onFailure { e ->
                _state.update { st -> st.copy(loading = false, error = e.message ?: "Error") }
            }
        }
    }

    fun selectTab(type: ContentType) = _state.update { st -> st.copy(tab = type) }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }
}
