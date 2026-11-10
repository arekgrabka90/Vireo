package app.vireo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import app.vireo.data.RecentSearchRepository
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbCatalogRepository
import app.vireo.data.tmdb.TmdbPersonSearchItem
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

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val results: List<TmdbCatalogItem> = emptyList(),
    val people: List<TmdbPersonSearchItem> = emptyList(),
    val recent: List<String> = emptyList(),
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

    init {
        viewModelScope.launch {
            profiles.activeIdFlow.collectLatest { profileId ->
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
        _state.value = _state.value.copy(query = value, error = null)
        searchJob?.cancel()

        if (value.isBlank()) {
            _state.value = _state.value.copy(
                loading = false,
                results = emptyList(),
                people = emptyList(),
            )
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
        viewModelScope.launch {
            recentSearchRepository.clear(profileId)
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

        searchJob = viewModelScope.launch {
            delay(debounceMs)
            performSearch(query.trim(), profileId, language)
        }
    }

    private suspend fun performSearch(
        query: String,
        profileId: Long,
        language: String,
    ) {
        _state.value = _state.value.copy(
            loading = true,
            error = null,
            results = emptyList(),
            people = emptyList(),
        )

        try {
            val result = repository.searchMulti(
                query = query,
                page = 1,
                language = language,
            )

            if (profileId != activeProfileId) return
            val data = result.getOrThrow()

            if (data.media.isNotEmpty() || data.people.isNotEmpty()) {
                recentSearchRepository.add(profileId, query)
            }

            if (profileId != activeProfileId) return

            _state.value = _state.value.copy(
                loading = false,
                results = data.media,
                people = data.people,
                error = if (data.media.isEmpty() && data.people.isEmpty()) {
                    "Nie znaleziono pasujących filmów, seriali ani osób."
                } else null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (profileId != activeProfileId) return
            _state.value = _state.value.copy(
                loading = false,
                error = error.message ?: "Wyszukiwanie nie powiodło się.",
            )
        }
    }

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
