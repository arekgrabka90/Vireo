package app.vireo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.tmdb.TmdbApi
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbCatalogRepository
import app.vireo.data.tmdb.TmdbDetail
import app.vireo.data.tmdb.TmdbDetailMapper
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    val heroDetails: Map<String, TmdbDetail> = emptyMap(),
)

@HiltViewModel
class TmdbHomeViewModel @Inject constructor(
    private val repository: TmdbCatalogRepository,
    private val api: TmdbApi,
) : ViewModel() {
    private val _state = MutableStateFlow(TmdbHomeState())
    val state: StateFlow<TmdbHomeState> = _state.asStateFlow()
    private var refreshGeneration = 0L
    private var refreshJob: Job? = null

    init { refresh() }

    fun refresh() {
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(loading = true, error = null, heroDetails = emptyMap())
            }

            val moviesResult = repository.trending(ContentType.MOVIE)
            val seriesResult = repository.trending(ContentType.SERIES)

            if (generation != refreshGeneration) return@launch

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

            val heroCandidates = (movies + series)
                .distinctBy { homeHeroKey(it.type, it.tmdbId) }
                .filter {
                    !it.backdropUrl.isNullOrBlank() ||
                        !it.posterUrl.isNullOrBlank()
                }
                .sortedByDescending { it.popularity }
                .take(5)

            val loadedDetails = coroutineScope {
                heroCandidates.map { item ->
                    async { loadHeroDetail(item) }
                }.awaitAll()
            }

            val detailMap = heroCandidates
                .zip(loadedDetails)
                .mapNotNull { (item, detail) ->
                    detail?.let {
                        homeHeroKey(item.type, item.tmdbId) to it
                    }
                }
                .toMap()

            if (generation == refreshGeneration) {
                _state.update { current ->
                    current.copy(heroDetails = detailMap)
                }
            }
        }
    }

    private suspend fun loadHeroDetail(item: TmdbCatalogItem): TmdbDetail? {
        return try {
            val dto = when (item.type) {
                ContentType.MOVIE -> api.movieDetails(item.tmdbId, "pl-PL")
                ContentType.SERIES -> api.seriesDetails(item.tmdbId, "pl-PL")
                ContentType.LIVE -> return null
            }

            val mediaType = if (item.type == ContentType.MOVIE) "movie" else "tv"
            val images = try {
                api.images(mediaType, item.tmdbId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }

            TmdbDetailMapper.map(dto, item.type, images)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
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
