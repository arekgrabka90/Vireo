package app.vireo.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.tmdb.TmdbDetail
import app.vireo.data.tmdb.TmdbDetailRepository
import app.vireo.data.LibraryRepository
import app.vireo.data.ProfileRepository
import app.vireo.data.xtream.TmdbXtreamAvailability
import app.vireo.data.xtream.TmdbXtreamAvailabilityRepository
import app.vireo.data.xtream.TmdbXtreamMatchVariant
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.SeriesDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TmdbDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: TmdbDetail? = null,
    val availabilityLoading: Boolean = false,
    val availabilityError: String? = null,
    val availability: TmdbXtreamAvailability? = null,
    val selectedSeries: SeriesDetails? = null,
    val selectedSourceId: Long? = null,
    val seriesLoading: Boolean = false,
    val playbackError: String? = null,
    val playbackReady: Boolean = false,
    val inMyList: Boolean = false,
)

@HiltViewModel
class TmdbDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TmdbDetailRepository,
    private val availabilityRepository: TmdbXtreamAvailabilityRepository,
    private val library: LibraryRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    val type: ContentType = runCatching {
        ContentType.valueOf(savedStateHandle.get<String>("type") ?: "MOVIE")
    }.getOrDefault(ContentType.MOVIE)

    val tmdbId: Int = savedStateHandle.get<String>("id")?.toIntOrNull() ?: 0
    val fallbackName: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow(TmdbDetailState())
    val state: StateFlow<TmdbDetailState> = _state.asStateFlow()

    private var seriesLoadJob: Job? = null
    private var seriesRequestId: Long = 0L

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = TmdbDetailState(loading = true)
            repository.details(type, tmdbId)
                .onSuccess { detail ->
                    val contentKey = "tmdb:${detail.type.name}:${detail.tmdbId}"
                    val inMyList = library.isWatchlisted(profiles.activeId, contentKey)
                    _state.value = TmdbDetailState(
                        loading = false,
                        detail = detail,
                        availabilityLoading = true,
                        inMyList = inMyList,
                    )
                    availabilityRepository.find(detail)
                        .onSuccess { availability ->
                            _state.value = _state.value.copy(
                                availabilityLoading = false,
                                availability = availability,
                            )
                        }
                        .onFailure { error ->
                            _state.value = _state.value.copy(
                                availabilityLoading = false,
                                availabilityError = error.message
                                    ?: "Nie udało się sprawdzić źródeł Xtream.",
                            )
                        }
                }
                .onFailure {
                    _state.value = TmdbDetailState(
                        loading = false,
                        error = it.message ?: "Nie udało się pobrać szczegółów TMDB.",
                    )
                }
        }
    }

    fun toggleMyList() {
        val detail = _state.value.detail ?: return
        val enabled = !_state.value.inMyList
        val contentKey = "tmdb:${detail.type.name}:${detail.tmdbId}"
        val profileId = profiles.activeId

        viewModelScope.launch {
            library.setWatchlisted(
                profileId = profileId,
                contentKey = contentKey,
                title = detail.title,
                contentType = detail.type.name,
                posterUrl = detail.posterUrl,
                backdropUrl = detail.backdropUrl,
                enabled = enabled,
            )
            _state.value = _state.value.copy(inMyList = enabled)
        }
    }

    fun playMovie(variant: TmdbXtreamMatchVariant, onReady: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(playbackError = null, playbackReady = false)
            availabilityRepository.playMovie(variant, _state.value.detail)
                .onSuccess {
                    _state.value = _state.value.copy(playbackReady = true)
                    onReady()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        playbackError = it.message ?: "Nie udało się uruchomić filmu.",
                    )
                }
        }
    }

    fun selectSeriesSource(variant: TmdbXtreamMatchVariant) {
        seriesLoadJob?.cancel()
        val requestId = ++seriesRequestId

        _state.value = _state.value.copy(
            selectedSeries = null,
            selectedSourceId = variant.sourceId,
            seriesLoading = true,
            playbackError = null,
        )

        seriesLoadJob = viewModelScope.launch {
            availabilityRepository.loadSeries(variant)
                .onSuccess { series ->
                    if (
                        requestId == seriesRequestId &&
                        _state.value.selectedSourceId == variant.sourceId
                    ) {
                        _state.value = _state.value.copy(
                            selectedSeries = series,
                            seriesLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    if (
                        requestId == seriesRequestId &&
                        _state.value.selectedSourceId == variant.sourceId
                    ) {
                        _state.value = _state.value.copy(
                            seriesLoading = false,
                            playbackError = error.message
                                ?: "Nie udało się pobrać odcinków.",
                        )
                    }
                }
        }
    }

    fun playEpisode(episodeId: String, onReady: () -> Unit) {
        val series = _state.value.selectedSeries ?: return
        if (availabilityRepository.playEpisode(series, episodeId, _state.value.detail)) {
            onReady()
        } else {
            _state.value = _state.value.copy(
                playbackError = "Nie znaleziono wybranego odcinka.",
            )
        }
    }
}
