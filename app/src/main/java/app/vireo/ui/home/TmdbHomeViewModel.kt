package app.vireo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbCatalogRepository
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TmdbHomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val movies: List<TmdbCatalogItem> = emptyList(),
    val series: List<TmdbCatalogItem> = emptyList(),
)

@HiltViewModel
class TmdbHomeViewModel @Inject constructor(
    private val repository: TmdbCatalogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TmdbHomeState())
    val state: StateFlow<TmdbHomeState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val moviesResult = repository.trending(ContentType.MOVIE)
            val seriesResult = repository.trending(ContentType.SERIES)

            val movies = moviesResult.getOrNull()?.items.orEmpty()
            val series = seriesResult.getOrNull()?.items.orEmpty()
            val errors = listOfNotNull(
                moviesResult.exceptionOrNull()?.message,
                seriesResult.exceptionOrNull()?.message,
            )

            _state.value = TmdbHomeState(
                loading = false,
                error = if (movies.isEmpty() && series.isEmpty()) {
                    errors.firstOrNull() ?: "TMDB nie zwróciło wyników."
                } else null,
                movies = movies,
                series = series,
            )
        }
    }
}

internal fun TmdbCatalogItem.toHomeTitle(): Title = Title(
    id = tmdbId.toString(),
    name = title,
    posterUrl = posterUrl,
    rating = rating?.let { String.format(java.util.Locale.US, "%.1f", it) },
    type = type,
    categoryId = null,
)
