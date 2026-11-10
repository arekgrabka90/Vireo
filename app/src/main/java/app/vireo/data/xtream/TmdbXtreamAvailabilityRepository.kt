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
import app.vireo.player.playbackRequestIdentity
import app.vireo.player.nextFailoverCandidate
import app.vireo.player.exactEpisodeFailoverCandidates
import app.vireo.player.PlaybackSourceCandidate
import app.vireo.player.PlaybackRequestIdentity
import app.vireo.player.EpisodeFailoverCandidate
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
                    sourceId = variant.sourceId,
                    streamId = variant.streamId,
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

    suspend fun playEpisodeByNumber(
        series: SeriesDetails,
        seasonNumber: Int,
        episodeNumber: Int,
        detail: TmdbDetail?,
        variants: List<TmdbXtreamMatchVariant>,
        selectedSourceId: Long,
    ): Boolean {
        if (seasonNumber < 0 || episodeNumber <= 0) return false

        val matches = series.seasons
            .filter { it.number == seasonNumber }
            .flatMap { it.episodes }
            .filter { it.number == episodeNumber }

        val selected = matches.singleOrNull()
            ?.takeIf { it.id.isNotBlank() && it.streamUrl.isNotBlank() }
            ?: return false

        return startEpisodeQueue(
            series = series,
            detail = detail,
            variants = variants,
            selectedSourceId = selectedSourceId,
            targetSeasonNumber = seasonNumber,
            targetEpisodeNumber = selected.number,
        )
    }

    suspend fun playEpisode(
        series: SeriesDetails,
        episodeId: String,
        detail: TmdbDetail?,
        variants: List<TmdbXtreamMatchVariant>,
        selectedSourceId: Long,
    ): Boolean {
        if (episodeId.isBlank()) return false

        val matches = series.seasons.flatMap { season ->
            season.episodes
                .filter { it.id == episodeId }
                .map { season.number to it }
        }

        val selected = matches.singleOrNull()
            ?.takeIf { it.second.streamUrl.isNotBlank() }
            ?: return false

        return startEpisodeQueue(
            series = series,
            detail = detail,
            variants = variants,
            selectedSourceId = selectedSourceId,
            targetSeasonNumber = selected.first,
            targetEpisodeNumber = selected.second.number,
        )
    }

    suspend fun playNextEpisode(
        series: SeriesDetails,
        detail: TmdbDetail,
        watchedEpisodeKeys: Set<String>,
        variants: List<TmdbXtreamMatchVariant>,
        selectedSourceId: Long,
    ): Boolean = startEpisodeQueue(
        series = series,
        detail = detail,
        variants = variants,
        selectedSourceId = selectedSourceId,
        watchedEpisodeKeys = watchedEpisodeKeys,
    )

    private suspend fun startEpisodeQueue(
        series: SeriesDetails,
        detail: TmdbDetail?,
        variants: List<TmdbXtreamMatchVariant>,
        selectedSourceId: Long,
        targetSeasonNumber: Int? = null,
        targetEpisodeNumber: Int? = null,
        watchedEpisodeKeys: Set<String>? = null,
    ): Boolean {
        if (selectedSourceId <= 0L || series.id.isBlank()) return false

        val playableEpisodes = series.seasons
            .sortedBy { it.number }
            .flatMap { season ->
                season.episodes
                    .sortedBy { it.number }
                    .map { season.number to it }
            }
            .filter { (seasonNumber, episode) ->
                seasonNumber >= 0 &&
                    episode.number > 0 &&
                    episode.id.isNotBlank() &&
                    episode.streamUrl.isNotBlank()
            }

        val queueEpisodes = playableEpisodes.filterNot { (season, episode) ->
            if (watchedEpisodeKeys == null || detail == null) {
                false
            } else {
                val key =
                    "tmdb:${detail.type.name}:${detail.tmdbId}:S${season}E${episode.number}"
                key in watchedEpisodeKeys
            }
        }

        if (queueEpisodes.isEmpty()) return false

        val selectedIndex = if (
            targetSeasonNumber != null && targetEpisodeNumber != null
        ) {
            queueEpisodes.indexOfFirst { (season, episode) ->
                season == targetSeasonNumber &&
                    episode.number == targetEpisodeNumber
            }
        } else {
            0
        }

        if (selectedIndex !in queueEpisodes.indices) return false

        // Pobieramy katalogi alternatywnych źródeł tylko raz na sesję.
        // Niepewne dopasowania nie są używane automatycznie.
        val alternativeSeries = mutableListOf<Pair<TmdbXtreamMatchVariant, SeriesDetails>>()

        val confirmedVariants = variants
            .asSequence()
            .filter {
                it.confidence != TmdbXtreamMatchConfidence.NEEDS_REVIEW &&
                    it.sourceId > 0L &&
                    it.streamId.isNotBlank()
            }
            .filterNot {
                it.sourceId == selectedSourceId && it.streamId == series.id
            }
            .distinctBy { it.sourceId to it.streamId }
            .toList()

        for (variant in confirmedVariants) {
            try {
                val account = accountDao.getById(variant.sourceId) ?: continue
                val provider = providers.create(account)
                val loaded = provider.seriesDetails(variant.streamId).getOrThrow()

                if (loaded.id != variant.streamId) continue
                alternativeSeries += variant to loaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Brak dostępu do alternatywnego katalogu nie blokuje źródła głównego.
            }
        }

        val items = queueEpisodes.map { (seasonNumber, episode) ->
            val rawCandidates = alternativeSeries.mapNotNull { (variant, otherSeries) ->
                val exactMatches = otherSeries.seasons
                    .asSequence()
                    .filter { it.number == seasonNumber }
                    .flatMap { it.episodes.asSequence() }
                    .filter { it.number == episode.number }
                    .toList()

                val alternativeEpisode = exactMatches.singleOrNull()
                    ?.takeIf {
                        it.id.isNotBlank() && it.streamUrl.isNotBlank()
                    }
                    ?: return@mapNotNull null

                EpisodeFailoverCandidate(
                    sourceId = variant.sourceId,
                    streamId = alternativeEpisode.id,
                    seasonNumber = seasonNumber,
                    episodeNumber = episode.number,
                    url = alternativeEpisode.streamUrl,
                )
            }

            val candidates = exactEpisodeFailoverCandidates(
                seasonNumber = seasonNumber,
                episodeNumber = episode.number,
                candidates = rawCandidates,
            )

            val currentIdentity = playbackRequestIdentity(episode.streamUrl)
            val attempted = mutableSetOf<PlaybackRequestIdentity>()
            val alternatives = mutableListOf<PlaybackAlternative>()

            while (true) {
                val next = nextFailoverCandidate(
                    candidates = candidates,
                    current = currentIdentity,
                    attempted = attempted,
                ) ?: break

                PlaybackAlternative(
                    sourceId = next.sourceId,
                    streamId = next.streamId,
                    url = next.url,
                    seriesId = alternativeSeries.firstOrNull {
                        it.first.sourceId == next.sourceId
                    }?.first?.streamId,
                    seasonNumber = next.seasonNumber,
                    episodeNumber = next.episodeNumber,
                )

                val identity = playbackRequestIdentity(next.url, next.headers)
                    ?: break
                attempted += identity
            }

            PlayItem(
                title = "${series.name} · S${seasonNumber} E${episode.number} · ${episode.title}",
                url = episode.streamUrl,
                contentKey = detail?.let {
                    "tmdb:${it.type.name}:${it.tmdbId}:S${seasonNumber}E${episode.number}"
                },
                contentType = detail?.type?.name ?: "SERIES",
                posterUrl = detail?.posterUrl,
                backdropUrl = detail?.backdropUrl,
                alternatives = alternatives,
                sourceId = selectedSourceId,
                streamId = episode.id,
                seriesId = series.id,
                seasonNumber = seasonNumber,
                episodeNumber = episode.number,
            )
        }

        playbackSession.start(items, selectedIndex)
        return true
    }

}
