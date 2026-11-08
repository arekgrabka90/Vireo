package app.vireo.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbCatalogRepository
import app.vireo.domain.model.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TmdbCatalogState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val items: List<TmdbCatalogItem> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
)

@HiltViewModel
class TmdbCatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TmdbCatalogRepository,
) : ViewModel() {
    val type: ContentType = runCatching {
        ContentType.valueOf(savedStateHandle.get<String>("type") ?: "MOVIE")
    }.getOrDefault(ContentType.MOVIE)

    private val _state = MutableStateFlow(TmdbCatalogState())
    val state: StateFlow<TmdbCatalogState> = _state.asStateFlow()

    init { loadFirstPage() }

    fun loadFirstPage() {
        viewModelScope.launch {
            _state.value = TmdbCatalogState(loading = true)
            repository.trending(type, 1)
                .onSuccess { page ->
                    _state.value = TmdbCatalogState(
                        loading = false,
                        items = page.items,
                        page = page.page,
                        totalPages = page.totalPages,
                    )
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, error = error.message ?: "TMDB error")
                    }
                }
        }
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.loading || current.loadingMore ||
            current.page == 0 || current.page >= current.totalPages
        ) return

        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            repository.trending(type, current.page + 1)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loadingMore = false,
                            items = (it.items + page.items).distinctBy { item ->
                                item.type.name + ":" + item.tmdbId
                            },
                            page = page.page,
                            totalPages = page.totalPages,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loadingMore = false, error = error.message ?: "TMDB error")
                    }
                }
        }
    }
}
