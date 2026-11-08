package app.vireo.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbCatalogRepository
import app.vireo.domain.model.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val results: List<TmdbCatalogItem> = emptyList(),
    val recent: List<String> = emptyList(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: TmdbCatalogRepository,
    private val profiles: ProfileRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val preferences =
        context.getSharedPreferences("vireo_search_history", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        SearchState(recent = readHistory())
    )
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(value: String) {
        _state.update { it.copy(query = value, error = null) }
        searchJob?.cancel()

        if (value.isBlank()) {
            _state.update { it.copy(loading = false, results = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            performSearch(value.trim())
        }
    }

    fun searchRecent(value: String) {
        onQueryChanged(value)
    }

    fun clearHistory() {
        preferences.edit().remove(historyKey()).apply()
        _state.update { it.copy(recent = emptyList()) }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(loading = true, error = null) }

        try {
            val (movies, series) = coroutineScope {
                val movieTask = async {
                    repository.search(query, ContentType.MOVIE)
                }
                val seriesTask = async {
                    repository.search(query, ContentType.SERIES)
                }
                movieTask.await() to seriesTask.await()
            }

            val movieItems = movies.getOrNull()?.items.orEmpty()
            val seriesItems = series.getOrNull()?.items.orEmpty()
            val combined = (movieItems + seriesItems)
                .distinctBy { "${it.type.name}:${it.tmdbId}" }

            if (combined.isNotEmpty()) saveHistory(query)

            _state.update {
                it.copy(
                    loading = false,
                    results = combined,
                    recent = readHistory(),
                    error = if (combined.isEmpty()) {
                        movies.exceptionOrNull()?.message
                            ?: series.exceptionOrNull()?.message
                            ?: "Nie znaleziono pasujących filmów ani seriali."
                    } else null,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    loading = false,
                    error = error.message ?: "Wyszukiwanie nie powiodło się.",
                )
            }
        }
    }

    private fun historyKey(): String =
        "profile_${profiles.activeId}_recent_searches"

    private fun readHistory(): List<String> =
        preferences.getString(historyKey(), null)
            ?.split('\n')
            .orEmpty()
            .filter { it.isNotBlank() }
            .take(8)

    private fun saveHistory(query: String) {
        val updated = (listOf(query) + readHistory())
            .distinctBy { it.trim().lowercase() }
            .take(8)

        preferences.edit()
            .putString(historyKey(), updated.joinToString("\n"))
            .apply()
    }
}
