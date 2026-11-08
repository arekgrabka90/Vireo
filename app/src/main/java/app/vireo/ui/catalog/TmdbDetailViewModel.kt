package app.vireo.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.tmdb.TmdbDetail
import app.vireo.data.tmdb.TmdbDetailRepository
import app.vireo.domain.model.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TmdbDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: TmdbDetail? = null,
)

@HiltViewModel
class TmdbDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TmdbDetailRepository,
) : ViewModel() {
    val type: ContentType = runCatching {
        ContentType.valueOf(savedStateHandle.get<String>("type") ?: "MOVIE")
    }.getOrDefault(ContentType.MOVIE)

    val tmdbId: Int = savedStateHandle.get<String>("id")?.toIntOrNull() ?: 0
    val fallbackName: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow(TmdbDetailState())
    val state: StateFlow<TmdbDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = TmdbDetailState(loading = true)
            repository.details(type, tmdbId)
                .onSuccess { _state.value = TmdbDetailState(loading = false, detail = it) }
                .onFailure {
                    _state.value = TmdbDetailState(
                        loading = false,
                        error = it.message ?: "Nie udało się pobrać szczegółów TMDB.",
                    )
                }
        }
    }
}
