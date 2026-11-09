package app.vireo.data.xtream

import app.vireo.data.ProviderRegistry
import app.vireo.data.local.AccountDao
import app.vireo.data.tmdb.TmdbDetail
import app.vireo.data.xtream.TmdbXtreamMatchConfidence
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.SeriesDetails
import app.vireo.player.PlayItem
import app.vireo.player.PlaybackAlternative
import app.vireo.player.PlaybackSession
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

data class TmdbXtreamAvailability(
    val match: TmdbXtreamMatch,
    val sourceNames: Map<Long, String>,
    val syncReport: XtreamIndexSyncReport,
)

@Singleton
class TmdbXtreamAvailabilityRepository @Inject constructor(
    private val indexRepository: XtreamContentIndexRepository,
    private val accountDao: AccountDao,
    private val providers: ProviderRegistry,
    private val playbackSession: PlaybackSession,
) {
    suspend fun find(detail: TmdbDetail): Result<TmdbXtreamAvailability> = try {
        if (detail.type != ContentType.MOVIE && detail.type != ContentType.SERIES) {
            error("Nieobsługiwany typ treści.")
        }

        // Odśwież wyłącznie indeks odpowiadający aktualnemu typowi TMDB.
        // Polityka TTL ogranicza pobrania, a raport ujawnia niedostępne źródła.
        val syncReport = when (detail.type) {
            ContentType.MOVIE -> indexRepository.syncMovies()
            ContentType.SERIES -> indexRepository.syncSeries()
            ContentType.LIVE -> error("Nieobsługiwany typ treści.")
        }

        val candidates = linkedMapOf<Pair<Long, String>, app.vireo.data.local.XtreamContentIndexEntity>()

        indexRepository.findCandidatesByExternalIds(
            tmdbId = detail.tmdbId,
            imdbId = detail.imdbId,
            type = detail.type,
        ).forEach { candidates[it.sourceId to it.streamId] = it }

        suspend fun collect(title: String?) {
            val clean = title?.trim()?.takeIf { it.isNotBlank() } ?: return
            indexRepository.findCandidates(clean, detail.type, detail.year)
                .forEach { candidates[it.sourceId to it.streamId] = it }
        }

        collect(detail.title)
        if (detail.originalTitle != detail.title) collect(detail.originalTitle)

        val catalogItem = app.vireo.data.tmdb.TmdbCatalogItem(
            tmdbId = detail.tmdbId,
            type = detail.type,
            title = detail.title,
            originalTitle = detail.originalTitle,
            releaseDate = detail.releaseDate,
            year = detail.year,
            overview = detail.overview,
            posterUrl = detail.posterUrl,
            backdropUrl = detail.backdropUrl,
            rating = detail.rating,
            voteCount = detail.voteCount,
            genreIds = emptyList(),
            originalLanguage = null,
            popularity = 0.0,
            imdbId = detail.imdbId,
        )

        val match = TmdbXtreamMatcher.match(catalogItem, candidates.values)
        val accounts = accountDao.getAll().associate { it.id to it.name }

        Result.success(
            TmdbXtreamAvailability(
                match = match,
                sourceNames = accounts,
                syncReport = syncReport,
            )
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun playMovie(
        variant: TmdbXtreamMatchVariant,
        alternatives: List<TmdbXtreamMatchVariant>,
        detail: TmdbDetail?,
    ): Result<Unit> = try {
        val account = accountDao.getById(variant.sourceId)
            ?: error("Konto źródłowe nie jest już dostępne.")
        val provider = providers.create(account)
        val movie = provider.movieDetails(variant.streamId).getOrThrow()

        if (movie.streamUrl.isBlank()) {
            error("Wybrane źródło nie ma prawidłowego adresu strumienia.")
        }

        // Alternatywy pochodzą wyłącznie z potwierdzonych dopasowań
        // tego samego tytułu TMDB. Niepewne warianty wymagają ręcznej selekcji.
        val alternativeUrls = buildList {
            val seenUrls = mutableSetOf(movie.streamUrl)

            for (candidate in alternatives) {
                if (candidate.sourceId == variant.sourceId &&
                    candidate.streamId == variant.streamId
                ) continue

                if (candidate.confidence ==
                    TmdbXtreamMatchConfidence.NEEDS_REVIEW
                ) continue

                try {
                    val alternativeAccount =
                        accountDao.getById(candidate.sourceId) ?: continue
                    val alternativeProvider = providers.create(alternativeAccount)
                    val alternativeMovie =
                        alternativeProvider.movieDetails(candidate.streamId).getOrThrow()

                    val url = alternativeMovie.streamUrl.trim()
                    if (url.isBlank() || !seenUrls.add(url)) continue

                    add(
                        PlaybackAlternative(
                            sourceId = candidate.sourceId,
                            streamId = candidate.streamId,
                            url = url,
                        )
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Niedostępny wariant pomijamy.
                    // Nie blokuje to odtwarzania głównego źródła.
                }
            }
        }

        playbackSession.start(
            listOf(
                PlayItem(
                    title = movie.name,
                    url = movie.streamUrl,
                    contentKey = detail?.let { "tmdb:${it.type.name}:${it.tmdbId}" },
                    contentType = detail?.type?.name ?: "MOVIE",
                    posterUrl = detail?.posterUrl,
                    backdropUrl = detail?.backdropUrl,
                    alternatives = alternativeUrls,
                )
            ),
            0,
        )
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun loadSeries(
        variant: TmdbXtreamMatchVariant,
    ): Result<SeriesDetails> = try {
        val account = accountDao.getById(variant.sourceId)
            ?: error("Konto źródłowe nie jest już dostępne.")
        val provider = providers.create(account)
        provider.seriesDetails(variant.streamId)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    fun playEpisodeByNumber(
        series: SeriesDetails,
        seasonNumber: Int,
        episodeNumber: Int,
        detail: TmdbDetail?,
    ): Boolean {
        if (seasonNumber < 0 || episodeNumber <= 0) return false

        // Dopasowanie po numerach, nigdy po pozycji listy ani TMDB ID.
        val matches = series.seasons
            .asSequence()
            .filter { it.number == seasonNumber }
            .flatMap { it.episodes.asSequence() }
            .filter { it.number == episodeNumber }
            .toList()

        // Zero lub więcej niż jeden wynik oznacza brak pewnego dopasowania.
        val match = matches.singleOrNull()
            ?.takeIf { it.id.isNotBlank() && it.streamUrl.isNotBlank() }
            ?: return false

        // Do playback przekazujemy wyłącznie poprawne ID odcinka Xtream.
        return playEpisode(series, match.id, detail)
    }

    fun playEpisode(
        series: SeriesDetails,
        episodeId: String,
        detail: TmdbDetail?,
    ): Boolean {
        val episodes = series.seasons.flatMap { season ->
            season.episodes.map { season.number to it }
        }
        if (episodeId.isBlank()) return false

        // Nie uruchamiamy pustych URL-i ani niejednoznacznych ID.
        val playableEpisodes = episodes.filter { (_, episode) ->
            episode.id.isNotBlank() && episode.streamUrl.isNotBlank()
        }
        val matchingEpisodes = playableEpisodes.filter {
            it.second.id == episodeId
        }
        val selected = matchingEpisodes.singleOrNull() ?: return false
        val index = playableEpisodes.indexOf(selected)

        val items = playableEpisodes.map { (season, episode) ->
            PlayItem(
                title = "${series.name} · S${season} E${episode.number} · ${episode.title}",
                url = episode.streamUrl,
                contentKey = detail?.let {
                    "tmdb:${it.type.name}:${it.tmdbId}:S${season}E${episode.number}"
                },
                contentType = detail?.type?.name ?: "SERIES",
                posterUrl = detail?.posterUrl,
                backdropUrl = detail?.backdropUrl,
            )
        }
        playbackSession.start(items, index)
        return true
    }

    fun playNextEpisode(
        series: SeriesDetails,
        detail: TmdbDetail,
        watchedEpisodeKeys: Set<String>,
    ): Boolean {
        val episodes = series.seasons
            .sortedBy { it.number }
            .flatMap { season ->
                season.episodes
                    .sortedBy { it.number }
                    .map { season.number to it }
            }

        if (episodes.isEmpty()) return false

        val nextEpisodes = episodes.filter { (_, episode) ->
            episode.id.isNotBlank() && episode.streamUrl.isNotBlank()
        }.filterNot { (season, episode) ->
            val key =
                "tmdb:${detail.type.name}:${detail.tmdbId}:S${season}E${episode.number}"
            key in watchedEpisodeKeys
        }

        if (nextEpisodes.isEmpty()) return false

        val items = nextEpisodes.map { (season, episode) ->
            PlayItem(
                title = "${series.name} · S${season} E${episode.number} · ${episode.title}",
                url = episode.streamUrl,
                contentKey =
                    "tmdb:${detail.type.name}:${detail.tmdbId}:S${season}E${episode.number}",
                contentType = detail.type.name,
                posterUrl = detail.posterUrl,
                backdropUrl = detail.backdropUrl,
            )
        }

        playbackSession.start(items, 0)
        return true
    }
}
