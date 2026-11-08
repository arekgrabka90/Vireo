package app.vireo.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.tmdb.TmdbDetail
import app.vireo.data.tmdb.TmdbDetailRepository
import app.vireo.data.xtream.TmdbXtreamAvailability
import app.vireo.data.xtream.TmdbXtreamAvailabilityRepository
import app.vireo.data.xtream.TmdbXtreamMatchVariant
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.SeriesDetails
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
    val availabilityLoading: Boolean = false,
    val availabilityError: String? = null,
    val availability: TmdbXtreamAvailability? = null,
    val selectedSeries: SeriesDetails? = null,
    val selectedSourceId: Long? = null,
    val playbackError: String? = null,
    val playbackReady: Boolean = false,
)

@HiltViewModel
class TmdbDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TmdbDetailRepository,
    private val availabilityRepository: TmdbXtreamAvailabilityRepository,
) : ViewModel() {
    val type: ContentType = runCatching {
        ContentType.valueOf(savedStateHandle.get<String>("type") ?: "MOVIE")
    }.getOrDefault(ContentType.MOVIE)

    val tmdbId: Int = savedStateHandle.get<String>("id")?.toIntOrNull() ?: 0
    val fallbackName: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow(TmdbDetailState())
    val state: StateFlow<TmdbDetailState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = TmdbDetailState(loading = true)
            repository.details(type, tmdbId)
                .onSuccess { detail ->
                    _state.value = TmdbDetailState(
                        loading = false,
                        detail = detail,
                        availabilityLoading = true,
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

    fun playMovie(variant: TmdbXtreamMatchVariant, onReady: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(playbackError = null, playbackReady = false)
            availabilityRepository.playMovie(variant)
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
        viewModelScope.launch {
            _state.value = _state.value.copy(
                selectedSeries = null,
                selectedSourceId = variant.sourceId,
                playbackError = null,
            )
            availabilityRepository.loadSeries(variant)
                .onSuccess {
                    _state.value = _state.value.copy(selectedSeries = it)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        playbackError = it.message ?: "Nie udało się pobrać odcinków.",
                    )
                }
        }
    }

    fun playEpisode(episodeId: String, onReady: () -> Unit) {
        val series = _state.value.selectedSeries ?: return
        if (availabilityRepository.playEpisode(series, episodeId)) {
            onReady()
        } else {
            _state.value = _state.value.copy(
                playbackError = "Nie znaleziono wybranego odcinka.",
            )
        }
    }
}
