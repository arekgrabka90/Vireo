package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbDetailRepository @Inject constructor(
    private val api: TmdbApi,
) {
    private data class Key(val type: ContentType, val id: Int, val language: String)
    private data class Entry(val detail: TmdbDetail, val savedAt: Long)
    private data class SeasonKey(val seriesId: Int, val seasonNumber: Int, val language: String)
    private data class SeasonEntry(val episodes: List<TmdbEpisodeItem>, val savedAt: Long)

    private val cache = ConcurrentHashMap<Key, Entry>()
    private val seasonCache = ConcurrentHashMap<SeasonKey, SeasonEntry>()

    suspend fun details(
        type: ContentType,
        id: Int,
        language: String = DEFAULT_LANGUAGE,
    ): Result<TmdbDetail> {
        if (type != ContentType.MOVIE && type != ContentType.SERIES) {
            return Result.failure(IllegalArgumentException("Unsupported TMDB content type."))
        }
        if (id <= 0) {
            return Result.failure(IllegalArgumentException("Invalid TMDB ID."))
        }

        val normalizedLanguage = language.trim().replace('_', '-')
            .ifBlank { DEFAULT_LANGUAGE }
        val key = Key(type, id, normalizedLanguage)
        val now = System.currentTimeMillis()

        cache[key]?.let {
            if (now - it.savedAt < CACHE_TTL_MS) return Result.success(it.detail)
            cache.remove(key, it)
        }

        return try {
            val dto = when (type) {
                ContentType.MOVIE -> api.movieDetails(id, normalizedLanguage)
                ContentType.SERIES -> api.seriesDetails(id, normalizedLanguage)
                ContentType.LIVE -> error("Unsupported content type.")
            }
            val mediaType = if (type == ContentType.MOVIE) "movie" else "tv"
            val images = try {
                api.images(mediaType, id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }

            val detail = TmdbDetailMapper.map(dto, type, images)
                ?: return Result.failure(IllegalStateException("TMDB returned invalid details."))

            cache[key] = Entry(detail, System.currentTimeMillis())
            trimCache()
            Result.success(detail)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun seasonEpisodes(
        seriesId: Int,
        seasonNumber: Int,
        language: String = DEFAULT_LANGUAGE,
    ): Result<List<TmdbEpisodeItem>> {
        if (seriesId <= 0 || seasonNumber < 0) {
            return Result.failure(IllegalArgumentException("Invalid TMDB season reference."))
        }

        val normalizedLanguage = language.trim().replace('_', '-')
            .ifBlank { DEFAULT_LANGUAGE }
        val key = SeasonKey(seriesId, seasonNumber, normalizedLanguage)
        val now = System.currentTimeMillis()

        seasonCache[key]?.let {
            if (now - it.savedAt < CACHE_TTL_MS) {
                return Result.success(it.episodes)
            }
            seasonCache.remove(key, it)
        }

        return try {
            val dto = api.seasonDetails(seriesId, seasonNumber, normalizedLanguage)
            if (dto.seasonNumber != seasonNumber) {
                return Result.failure(
                    IllegalStateException("TMDB returned a different season number.")
                )
            }

            val episodes = dto.episodes
                .asSequence()
                .filter { it.seasonNumber == seasonNumber }
                .mapNotNull(TmdbDetailMapper::mapEpisode)
                .distinctBy { it.episodeNumber }
                .sortedBy { it.episodeNumber }
                .toList()

            seasonCache[key] = SeasonEntry(episodes, System.currentTimeMillis())
            trimSeasonCache()
            Result.success(episodes)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun trimSeasonCache() {
        while (seasonCache.size > MAX_SEASON_CACHE_ENTRIES) {
            val oldest = seasonCache.entries.minByOrNull { it.value.savedAt } ?: return
            seasonCache.remove(oldest.key, oldest.value)
        }
    }

    private fun trimCache() {
        if (cache.size <= MAX_CACHE_ENTRIES) return
        val oldest = cache.entries.minByOrNull { it.value.savedAt } ?: return
        cache.remove(oldest.key, oldest.value)
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "pl-PL"
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 100
        private const val MAX_SEASON_CACHE_ENTRIES = 120
    }
}
