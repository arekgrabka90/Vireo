package app.vireo.data.xtream

import app.vireo.data.ProviderRegistry
import app.vireo.data.local.AccountDao
import app.vireo.data.tmdb.TmdbDetail
import app.vireo.data.tmdb.TmdbDetailRepository
import app.vireo.data.tmdb.TmdbEpisodeItem
import app.vireo.data.xtream.TmdbXtreamMatchConfidence
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.SeriesDetails
import app.vireo.player.PlayItem
import app.vireo.player.PlaybackAlternative
import app.vireo.player.PlaybackEpisode
import app.vireo.player.PlaybackSession
import app.vireo.player.playbackRequestIdentity
import app.vireo.player.nextFailoverCandidate
import app.vireo.player.exactEpisodeFailoverCandidates
import app.vireo.player.PlaybackSourceCandidate
import app.vireo.player.PlaybackRequestIdentity
import app.vireo.player.EpisodeFailoverCandidate
import app.vireo.player.exactEpisodeSeriesIds
import app.vireo.player.EpisodeFailoverSeriesCandidate
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
    private val tmdbDetailRepository: TmdbDetailRepository,
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
        val tmdb = detail?.takeIf { it.type == ContentType.MOVIE }
            ?: error("Brak prawidłowych szczegółów filmu z TMDB.")

        val account = accountDao.getById(variant.sourceId)
            ?: error("Konto źródłowe nie jest już dostępne.")
        val provider = providers.create(account)
        val movie = provider.movieDetails(variant.streamId).getOrThrow()
        val streamUrl = movie.streamUrl.trim()

        if (streamUrl.isBlank()) {
            error("Wybrane źródło nie ma prawidłowego adresu strumienia.")
        }

        // Xtream dostarcza adres odtwarzania, nie metadane filmu.
        val alternativeUrls = buildList {
            val seenUrls = mutableSetOf(streamUrl)

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
                    // Niedostępne źródło alternatywne pomijamy.
                }
            }
        }

        playbackSession.start(
            listOf(
                PlayItem(
                    title = tmdb.title,
                    url = streamUrl,
                    contentKey = "tmdb:${tmdb.type.name}:${tmdb.tmdbId}",
                    contentType = tmdb.type.name,
                    posterUrl = tmdb.posterUrl,
                    logoUrl = tmdb.logoUrl,
                    backdropUrl = tmdb.backdropUrl,
                    alternatives = alternativeUrls,
                    sourceId = variant.sourceId,
                    streamId = variant.streamId,
                    description = tmdb.overview,
                    year = tmdb.year?.toString(),
                    genre = tmdb.genres.joinToString(", ")
                        .takeIf { it.isNotBlank() },
                    duration = tmdb.runtimeMinutes?.let { "$it min" },
                    rating = tmdb.rating?.toString(),
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
        val tmdb = detail?.takeIf { it.type == ContentType.SERIES }
            ?: return false

        if (selectedSourceId <= 0L || series.id.isBlank()) return false

        // TMDB jest katalogiem odcinków. Xtream służy wyłącznie
        // do znalezienia odpowiadającego mu strumienia.
        val tmdbEpisodes = mutableListOf<TmdbEpisodeItem>()
        for (season in tmdb.seasons.sortedBy { it.seasonNumber }) {
            val result = tmdbDetailRepository.seasonEpisodes(
                seriesId = tmdb.tmdbId,
                seasonNumber = season.seasonNumber,
            )
            val seasonItems = result.getOrElse { error ->
                if (error is CancellationException) throw error
                return false
            }
            tmdbEpisodes.addAll(seasonItems)
        }

        val playableEpisodes = mapTmdbEpisodeStreams(tmdbEpisodes, series)
        if (playableEpisodes.isEmpty()) return false

        val queueEpisodes = playableEpisodes.filterNot { match ->
            if (watchedEpisodeKeys == null) {
                false
            } else {
                val episode = match.tmdbEpisode
                val key =
                    "tmdb:${tmdb.type.name}:${tmdb.tmdbId}:S${episode.seasonNumber}E${episode.episodeNumber}"
                key in watchedEpisodeKeys
            }
        }

        if (queueEpisodes.isEmpty()) return false

        val selectedIndex = if (
            targetSeasonNumber != null && targetEpisodeNumber != null
        ) {
            queueEpisodes.indexOfFirst { match ->
                match.tmdbEpisode.seasonNumber == targetSeasonNumber &&
                    match.tmdbEpisode.episodeNumber == targetEpisodeNumber
            }
        } else {
            0
        }

        if (selectedIndex !in queueEpisodes.indices) return false

        // Alternatywne źródła dostarczają wyłącznie adresy strumieni.
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
                // Niedostępny katalog alternatywny nie blokuje źródła głównego.
            }
        }

        val items = queueEpisodes.map { match ->
            val tmdbEpisode = match.tmdbEpisode
            val streamEpisode = match.streamEpisode
            val seasonNumber = tmdbEpisode.seasonNumber
            val episodeNumber = tmdbEpisode.episodeNumber

            val rawCandidatesWithSeries = alternativeSeries.mapNotNull { (variant, otherSeries) ->
                val exactMatches = otherSeries.seasons
                    .asSequence()
                    .filter { it.number == seasonNumber }
                    .flatMap { it.episodes.asSequence() }
                    .filter { it.number == episodeNumber }
                    .toList()

                val alternativeEpisode = exactMatches.singleOrNull()
                    ?.takeIf { it.id.isNotBlank() && it.streamUrl.isNotBlank() }
                    ?: return@mapNotNull null

                EpisodeFailoverSeriesCandidate(
                    candidate = EpisodeFailoverCandidate(
                        sourceId = variant.sourceId,
                        streamId = alternativeEpisode.id,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        url = alternativeEpisode.streamUrl,
                    ),
                    seriesId = variant.streamId,
                )
            }

            val seriesIdsByCandidate = exactEpisodeSeriesIds(rawCandidatesWithSeries)
            val rawCandidates = rawCandidatesWithSeries
                .map { it.candidate }
                .filter {
                    Triple(it.sourceId, it.streamId, it.url) in seriesIdsByCandidate
                }

            val candidates = exactEpisodeFailoverCandidates(
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                candidates = rawCandidates,
            )

            val currentIdentity = playbackRequestIdentity(streamEpisode.streamUrl)
            val attempted = mutableSetOf<PlaybackRequestIdentity>()
            val alternatives = mutableListOf<PlaybackAlternative>()

            while (true) {
                val next = nextFailoverCandidate(
                    candidates = candidates,
                    current = currentIdentity,
                    attempted = attempted,
                ) ?: break

                alternatives += PlaybackAlternative(
                    sourceId = next.sourceId,
                    streamId = next.streamId,
                    url = next.url,
                    seriesId = seriesIdsByCandidate[Triple(next.sourceId, next.streamId, next.url)],
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                )

                val identity = playbackRequestIdentity(next.url, next.headers)
                    ?: break
                attempted += identity
            }

            PlayItem(
                title = tmdb.title,
                episodeTitle = tmdbEpisode.title.takeIf { it.isNotBlank() },
                url = streamEpisode.streamUrl,
                contentKey =
                    "tmdb:${tmdb.type.name}:${tmdb.tmdbId}:S${seasonNumber}E${episodeNumber}",
                contentType = tmdb.type.name,
                posterUrl = tmdb.posterUrl,
                logoUrl = tmdb.logoUrl,
                backdropUrl = tmdb.backdropUrl,
                alternatives = alternatives,
                sourceId = selectedSourceId,
                streamId = streamEpisode.id,
                seriesId = series.id,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                description = tmdb.overview,
                year = tmdb.year?.toString(),
                genre = tmdb.genres.joinToString(", ")
                    .takeIf { it.isNotBlank() },
                rating = tmdb.rating?.toString(),
                episodeDescription = tmdbEpisode.overview,
                duration = tmdbEpisode.runtimeMinutes?.let { "$it min" },
                episodeThumbnailUrl = tmdbEpisode.stillUrl,
                episodes = playableEpisodes.map { episodeMatch ->
                    val episode = episodeMatch.tmdbEpisode
                    PlaybackEpisode(
                        title = episode.title,
                        seasonNumber = episode.seasonNumber,
                        episodeNumber = episode.episodeNumber,
                        description = episode.overview,
                        duration = episode.runtimeMinutes?.let { "$it min" },
                        thumbnailUrl = episode.stillUrl,
                    )
                },
            )
        }

        playbackSession.start(items, selectedIndex)
        return true
    }

}
