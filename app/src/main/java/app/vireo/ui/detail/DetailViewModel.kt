package app.vireo.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.MovieDetails
import app.vireo.domain.model.SeriesDetails
import app.vireo.player.PlayItem
import app.vireo.player.PlaybackSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val movie: MovieDetails? = null,
    val series: SeriesDetails? = null,
    val season: Int = 1,
    val sourceId: Long? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ContentRepository,
    private val session: PlaybackSession,
) : ViewModel() {

    val type: ContentType = ContentType.valueOf(savedStateHandle.get<String>("type") ?: "MOVIE")
    private val id: String = savedStateHandle.get<String>("id").orEmpty()
    val name: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { st -> st.copy(loading = true, error = null) }
            val sourceId = repo.activeAccount()?.id
            _state.update { st -> st.copy(sourceId = sourceId) }

            if (type == ContentType.SERIES) {
                repo.seriesDetails(id)
                    .onSuccess { s ->
                        _state.update { st ->
                            st.copy(
                                loading = false,
                                series = s,
                                season = s.seasons.firstOrNull()?.number ?: 1,
                            )
                        }
                    }
                    .onFailure { e -> fail(e) }
            } else {
                repo.movieDetails(id)
                    .onSuccess { m -> _state.update { st -> st.copy(loading = false, movie = m) } }
                    .onFailure { e -> fail(e) }
            }
        }
    }

    private fun fail(e: Throwable) {
        _state.update { st -> st.copy(loading = false, error = e.message ?: "Error") }
    }

    fun selectSeason(number: Int) = _state.update { st -> st.copy(season = number) }

    fun playMovie() {
        val m = _state.value.movie ?: return
        session.start(
            listOf(
                PlayItem(
                    title = m.name,
                    url = m.streamUrl,
                    contentKey = "xtream:${type.name}:${m.id}",
                    contentType = type.name,
                    posterUrl = m.posterUrl,
                    backdropUrl = m.backdropUrl,
                    streamId = m.id,
                    sourceId = _state.value.sourceId,
                )
            ),
            0,
        )
    }

    fun playEpisode(season: Int, episodeId: String) {
        val s = _state.value.series ?: return
        val flat = s.seasons.flatMap { se -> se.episodes.map { ep -> se.number to ep } }
        val idx = flat.indexOfFirst { pair -> pair.first == season && pair.second.id == episodeId }
        val items = flat.map { pair ->
            PlayItem(
                title = "${s.name} · S${pair.first} E${pair.second.number} · ${pair.second.title}",
                url = pair.second.streamUrl,
                contentKey = "xtream:${type.name}:${s.id}:S${pair.first}E${pair.second.number}",
                contentType = type.name,
                posterUrl = s.posterUrl,
                backdropUrl = s.backdropUrl,
                streamId = pair.second.id,
                sourceId = _state.value.sourceId,
                seriesId = s.id,
                seasonNumber = pair.first,
                episodeNumber = pair.second.number,
            )
        }
        session.start(items, idx.coerceAtLeast(0))
    }
}
