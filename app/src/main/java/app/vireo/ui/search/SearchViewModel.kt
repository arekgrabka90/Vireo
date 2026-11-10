package app.vireo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import app.vireo.data.RecentSearchRepository
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbCatalogRepository
import app.vireo.data.tmdb.TmdbPersonSearchItem
import app.vireo.data.tmdb.TmdbMultiSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SearchError { FIRST_PAGE, PAGE, EMPTY }

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: SearchError? = null,
    val results: List<TmdbCatalogItem> = emptyList(),
    val people: List<TmdbPersonSearchItem> = emptyList(),
    val recent: List<String> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalResults: Int = 0,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: TmdbCatalogRepository,
    private val profiles: ProfileRepository,
    private val profileSettingsRepository: ProfileSettingsRepository,
    private val recentSearchRepository: RecentSearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var activeProfileId = 0L
    private var contentLanguage = DEFAULT_LANGUAGE
    private var requestGeneration = 0L

    init {
        viewModelScope.launch {
            profiles.activeIdFlow.collectLatest { profileId ->
                requestGeneration++
                searchJob?.cancel()
                activeProfileId = profileId
                contentLanguage = DEFAULT_LANGUAGE
                _state.value = SearchState()

                if (profileId <= 0L) return@collectLatest

                val profile = profiles.get(profileId)
                    ?: return@collectLatest

                profileSettingsRepository.ensure(
                    profileId = profile.id,
                    kidsMode = profile.kids,
                )

                combine(
                    profileSettingsRepository.observe(profileId),
                    recentSearchRepository.observe(profileId),
                ) { settings, recent ->
                    Pair(settings.contentLanguage, recent)
                }.collectLatest { (storedLanguage, recent) ->
                    val resolved = resolveLanguage(storedLanguage)
                    val changed = resolved != contentLanguage
                    contentLanguage = resolved

                    _state.value = _state.value.copy(recent = recent)

                    if (changed && _state.value.query.isNotBlank()) {
                        scheduleSearch(
                            _state.value.query,
                            profileId,
                            resolved,
                            0L,
                        )
                    }
                }
            }
        }
    }

    fun onQueryChanged(value: String) {
        searchJob?.cancel()
        _state.value = _state.value.copy(
            query = value,
            loading = value.isNotBlank(),
            loadingMore = false,
            error = null,
            results = emptyList(),
            people = emptyList(),
            page = 0,
            totalPages = 0,
            totalResults = 0,
        )

        if (value.isBlank()) {
            requestGeneration++
            return
        }

        scheduleSearch(
            value,
            activeProfileId,
            contentLanguage,
            SEARCH_DEBOUNCE_MS,
        )
    }

    fun searchRecent(value: String) = onQueryChanged(value)

    fun clearHistory() {
        val profileId = activeProfileId
        if (profileId <= 0L) return
        viewModelScope.launch {
            recentSearchRepository.clear(profileId)
        }
    }

    fun retry() {
        val current = _state.value
        if (current.query.isBlank()) return

        if (
            current.page > 0 &&
            current.page < current.totalPages &&
            (current.results.isNotEmpty() || current.people.isNotEmpty())
        ) {
            loadNextPage()
        } else {
            scheduleSearch(
                current.query,
                activeProfileId,
                contentLanguage,
                0L,
            )
        }
    }

    fun loadNextPage() {
        val current = _state.value
        if (
            current.loading ||
            current.loadingMore ||
            current.page <= 0 ||
            current.page >= current.totalPages ||
            activeProfileId <= 0L
        ) return

        searchJob?.cancel()
        val generation = ++requestGeneration
        val profileId = activeProfileId
        val query = current.query.trim()
        val language = contentLanguage
        val nextPage = current.page + 1

        _state.value = current.copy(
            loadingMore = true,
            error = null,
        )

        searchJob = viewModelScope.launch {
            try {
                val next = repository.searchMulti(
                    query = query,
                    page = nextPage,
                    language = language,
                ).getOrThrow()

                if (!isCurrent(generation, profileId, query)) return@launch

                val currentData = TmdbMultiSearchResult(
                    media = _state.value.results,
                    people = _state.value.people,
                    page = _state.value.page,
                    totalPages = _state.value.totalPages,
                    totalResults = _state.value.totalResults,
                )
                val merged = mergeSearchResults(currentData, next)

                _state.value = _state.value.copy(
                    results = merged.media,
                    people = merged.people,
                    page = merged.page,
                    totalPages = merged.totalPages,
                    totalResults = merged.totalResults,
                    loadingMore = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (isCurrent(generation, profileId, query)) {
                    _state.value = _state.value.copy(
                        loadingMore = false,
                        error = SearchError.PAGE,
                    )
                }
            }
        }
    }

    private fun scheduleSearch(
        query: String,
        profileId: Long,
        language: String,
        debounceMs: Long,
    ) {
        searchJob?.cancel()
        if (profileId <= 0L || query.isBlank()) return

        val generation = ++requestGeneration
        _state.value = _state.value.copy(
            loading = true,
            loadingMore = false,
            error = null,
        )

        searchJob = viewModelScope.launch {
            delay(debounceMs)
            performSearch(query.trim(), profileId, language, generation)
        }
    }

    private suspend fun performSearch(
        query: String,
        profileId: Long,
        language: String,
        generation: Long,
    ) {
        _state.value = _state.value.copy(
            loading = true,
            loadingMore = false,
            error = null,
            results = emptyList(),
            people = emptyList(),
            page = 0,
            totalPages = 0,
            totalResults = 0,
        )

        try {
            val data = repository.searchMulti(
                query = query,
                page = 1,
                language = language,
            ).getOrThrow()

            if (!isCurrent(generation, profileId, query)) return

            if (data.media.isNotEmpty() || data.people.isNotEmpty()) {
                recentSearchRepository.add(profileId, query)
            }

            if (!isCurrent(generation, profileId, query)) return

            _state.value = _state.value.copy(
                loading = false,
                loadingMore = false,
                results = data.media,
                people = data.people,
                page = data.page,
                totalPages = data.totalPages.coerceIn(0, 500),
                totalResults = data.totalResults,
                error = null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (isCurrent(generation, profileId, query)) {
                _state.value = _state.value.copy(
                    loading = false,
                    loadingMore = false,
                    error = SearchError.FIRST_PAGE,
                )
            }
        }
    }

    private fun isCurrent(
        generation: Long,
        profileId: Long,
        query: String,
    ): Boolean =
        generation == requestGeneration &&
            profileId == activeProfileId &&
            _state.value.query.trim() == query

    private fun resolveLanguage(value: String): String {
        val normalized = value.trim().replace('_', '-')
        return if (
            normalized.isBlank() ||
            normalized.equals("system", ignoreCase = true)
        ) {
            Locale.getDefault().toLanguageTag().ifBlank { DEFAULT_LANGUAGE }
        } else {
            normalized
        }
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "pl-PL"
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
