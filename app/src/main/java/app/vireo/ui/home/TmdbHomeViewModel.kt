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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
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
    private data class CachedHeroDetail(
        val detail: TmdbDetail,
        val savedAt: Long,
    )

    private val _state = MutableStateFlow(TmdbHomeState())
    val state: StateFlow<TmdbHomeState> = _state.asStateFlow()
    private val heroDetailCache = ConcurrentHashMap<String, CachedHeroDetail>()
    private val heroDetailSlots = Semaphore(permits = 2)
    private var refreshGeneration = 0L
    private var refreshJob: Job? = null

    init { refresh() }

    fun refresh() {
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob = viewModelScope.launch {
            // Keep the last usable catalog and its artwork visible during refresh.
            val previous = _state.value
            _state.update { it.copy(loading = true, error = null) }

            val (moviesResult, seriesResult) = coroutineScope {
                val movies = async { repository.trending(ContentType.MOVIE) }
                val series = async { repository.trending(ContentType.SERIES) }
                movies.await() to series.await()
            }

            if (generation != refreshGeneration) return@launch

            val movies = moviesResult.getOrNull()?.items ?: previous.movies
            val series = seriesResult.getOrNull()?.items ?: previous.series
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
                heroDetails = previous.heroDetails,
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
                    val key = homeHeroKey(item.type, item.tmdbId)
                    (detail ?: previous.heroDetails[key])?.let { key to it }
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
        val key = homeHeroKey(item.type, item.tmdbId)
        cachedHeroDetail(key)?.let { return it }

        return heroDetailSlots.withPermit {
            // A second cache check avoids duplicate requests queued for the same title.
            cachedHeroDetail(key)?.let { return@withPermit it }

            try {
                val dto = when (item.type) {
                    ContentType.MOVIE -> api.movieDetails(item.tmdbId, "pl-PL")
                    ContentType.SERIES -> api.seriesDetails(item.tmdbId, "pl-PL")
                    ContentType.LIVE -> return@withPermit null
                }

                val mediaType = if (item.type == ContentType.MOVIE) "movie" else "tv"
                val images = try {
                    api.images(mediaType, item.tmdbId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }

                TmdbDetailMapper.map(dto, item.type, images)?.also { detail ->
                    heroDetailCache[key] = CachedHeroDetail(
                        detail = detail,
                        savedAt = System.currentTimeMillis(),
                    )
                    trimHeroDetailCache()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun cachedHeroDetail(key: String): TmdbDetail? {
        val entry = heroDetailCache[key] ?: return null
        if (System.currentTimeMillis() - entry.savedAt < HERO_DETAIL_CACHE_TTL_MS) {
            return entry.detail
        }
        heroDetailCache.remove(key, entry)
        return null
    }

    private fun trimHeroDetailCache() {
        while (heroDetailCache.size > MAX_HERO_DETAIL_CACHE_ENTRIES) {
            val oldest = heroDetailCache.entries.minByOrNull { it.value.savedAt } ?: return
            heroDetailCache.remove(oldest.key, oldest.value)
        }
    }

    private companion object {
        const val HERO_DETAIL_CACHE_TTL_MS = 30 * 60 * 1000L
        const val MAX_HERO_DETAIL_CACHE_ENTRIES = 40
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
