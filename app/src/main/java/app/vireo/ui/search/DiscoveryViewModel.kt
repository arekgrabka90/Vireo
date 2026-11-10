package app.vireo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbDiscoveryCategory
import app.vireo.data.tmdb.TmdbDiscoveryFilters
import app.vireo.data.tmdb.TmdbDiscoveryRepository
import app.vireo.data.tmdb.TmdbGenre
import app.vireo.data.tmdb.TmdbGenreMatch
import app.vireo.data.tmdb.TmdbGenreRepository
import app.vireo.data.tmdb.TmdbDiscoverySort
import app.vireo.domain.model.ContentType
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
import kotlinx.coroutines.launch

data class DiscoveryState(
    val type: ContentType = ContentType.MOVIE,
    val category: TmdbDiscoveryCategory = TmdbDiscoveryCategory.TRENDING_WEEK,
    val filters: TmdbDiscoveryFilters = TmdbDiscoveryFilters(),
    val items: List<TmdbCatalogItem> = emptyList(),
    val genres: List<TmdbGenre> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val genresLoading: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val totalPages: Int = 0,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: TmdbDiscoveryRepository,
    private val genreRepository: TmdbGenreRepository,
    private val profiles: ProfileRepository,
    private val profileSettings: ProfileSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    private var activeProfileId = 0L
    private var contentLanguage = DEFAULT_LANGUAGE
    private var requestGeneration = 0L
    private var requestJob: Job? = null
    private var filterJob: Job? = null
    private var genreJob: Job? = null

    init {
        viewModelScope.launch {
            profiles.activeIdFlow.collectLatest { profileId ->
                activeProfileId = profileId
                requestJob?.cancel()
                filterJob?.cancel()
                _state.value = DiscoveryState()

                if (profileId <= 0L) return@collectLatest

                val profile = profiles.get(profileId)
                    ?: return@collectLatest

                profileSettings.ensure(
                    profileId = profile.id,
                    kidsMode = profile.kids,
                )

                var firstEmission = true

                profileSettings.observe(profileId).collectLatest { settings ->
                    val resolved = resolveLanguage(settings.contentLanguage)
                    val changed = resolved != contentLanguage
                    contentLanguage = resolved

                    if (firstEmission || changed) {
                        firstEmission = false
                        loadGenres()
                        loadFirstPage()
                    }
                }
            }
        }
    }

    fun selectType(type: ContentType) {
        if (type != ContentType.MOVIE && type != ContentType.SERIES) return
        if (_state.value.type == type) return

        _state.value = _state.value.copy(
            type = type,
            items = emptyList(),
            page = 0,
            totalPages = 0,
            error = null,
        )
        loadGenres()
        loadFirstPage()
    }

    fun selectCategory(category: TmdbDiscoveryCategory) {
        if (_state.value.category == category) return

        _state.value = _state.value.copy(
            category = category,
            filters = TmdbDiscoveryFilters(),
            items = emptyList(),
            page = 0,
            totalPages = 0,
            error = null,
        )
        loadFirstPage()
    }

    fun updateFilters(filters: TmdbDiscoveryFilters) {
        _state.value = _state.value.copy(
            category = TmdbDiscoveryCategory.CUSTOM,
            filters = filters,
            items = emptyList(),
            page = 0,
            totalPages = 0,
            error = null,
        )

        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(FILTER_DEBOUNCE_MS)
            loadFirstPage()
        }
    }

    fun setGenres(ids: List<Int>) =
        updateFilters(_state.value.filters.withGenres(ids))

    fun setGenreMatch(match: TmdbGenreMatch) =
        updateFilters(_state.value.filters.copy(genreMatch = match))

    fun setMinimumRating(value: Double?) =
        updateFilters(_state.value.filters.copy(minRating = value))

    fun setMinimumVotes(value: Int?) =
        updateFilters(_state.value.filters.copy(minVoteCount = value))

    fun setYear(value: Int?) =
        updateFilters(_state.value.filters.withYear(value))

    fun setDecade(value: Int?) =
        updateFilters(_state.value.filters.withDecade(value))

    fun setOriginalLanguage(value: String?) =
        updateFilters(_state.value.filters.withOriginalLanguage(value))

    fun setSort(value: TmdbDiscoverySort) =
        updateFilters(_state.value.filters.copy(sortBy = value))

    fun clearFilters() {
        updateFilters(TmdbDiscoveryFilters())
    }

    fun retry() {
        loadFirstPage()
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.loading || current.loadingMore ||
            current.page <= 0 || current.page >= current.totalPages ||
            activeProfileId <= 0L
        ) return

        val nextPage = current.page + 1
        val generation = requestGeneration
        val profileId = activeProfileId

        requestJob?.cancel()
        _state.value = current.copy(loadingMore = true, error = null)

        requestJob = viewModelScope.launch {
            try {
                val result = repository.discover(
                    category = current.category,
                    type = current.type,
                    filters = current.filters,
                    page = nextPage,
                    language = contentLanguage,
                ).getOrThrow()

                if (generation != requestGeneration ||
                    profileId != activeProfileId
                ) return@launch

                val latest = _state.value
                _state.value = latest.copy(
                    items = (latest.items + result.items)
                        .distinctBy { "${it.type.name}:${it.tmdbId}" },
                    page = result.page,
                    totalPages = result.totalPages,
                    loadingMore = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == requestGeneration &&
                    profileId == activeProfileId
                ) {
                    _state.value = _state.value.copy(
                        loadingMore = false,
                        error = error.message ?: "Nie udało się pobrać kolejnej strony.",
                    )
                }
            }
        }
    }

    private fun loadFirstPage() {
        if (activeProfileId <= 0L) return

        filterJob?.cancel()
        requestJob?.cancel()
        val generation = ++requestGeneration
        val profileId = activeProfileId
        val current = _state.value

        _state.value = current.copy(
            loading = true,
            loadingMore = false,
            items = emptyList(),
            page = 0,
            totalPages = 0,
            error = null,
        )

        requestJob = viewModelScope.launch {
            try {
                val result = repository.discover(
                    category = current.category,
                    type = current.type,
                    filters = current.filters,
                    page = 1,
                    language = contentLanguage,
                ).getOrThrow()

                if (generation != requestGeneration ||
                    profileId != activeProfileId
                ) return@launch

                _state.value = _state.value.copy(
                    loading = false,
                    items = result.items.distinctBy {
                        "${it.type.name}:${it.tmdbId}"
                    },
                    page = result.page,
                    totalPages = result.totalPages.coerceAtMost(500),
                    error = if (result.items.isEmpty()) {
                        "Brak wyników dla wybranych kryteriów."
                    } else null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == requestGeneration &&
                    profileId == activeProfileId
                ) {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = error.message ?: "Nie udało się pobrać katalogu.",
                    )
                }
            }
        }
    }

    private fun loadGenres() {
        genreJob?.cancel()
        val type = _state.value.type
        val language = contentLanguage
        val profileId = activeProfileId

        _state.value = _state.value.copy(
            genresLoading = true,
            genres = emptyList(),
        )

        genreJob = viewModelScope.launch {
            try {
                val genres = genreRepository.genres(type, language).getOrThrow()
                if (profileId == activeProfileId &&
                    type == _state.value.type &&
                    language == contentLanguage
                ) {
                    _state.value = _state.value.copy(
                        genres = genres,
                        genresLoading = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (profileId == activeProfileId) {
                    _state.value = _state.value.copy(
                        genresLoading = false,
                    )
                }
            }
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
        private const val FILTER_DEBOUNCE_MS = 300L
    }
}
