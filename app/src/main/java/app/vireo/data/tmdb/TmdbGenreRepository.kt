package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbGenreRepository @Inject constructor(
    private val api: TmdbApi,
) {
    private data class CacheKey(
        val type: ContentType,
        val language: String,
    )

    private data class CacheEntry(
        val genres: List<TmdbGenre>,
        val savedAt: Long,
    )

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()

    suspend fun genres(
        type: ContentType,
        language: String = DEFAULT_LANGUAGE,
    ): Result<List<TmdbGenre>> {
        if (type != ContentType.MOVIE && type != ContentType.SERIES) {
            return Result.failure(
                IllegalArgumentException("Genres support movies and series only.")
            )
        }

        val normalizedLanguage = language.trim()
            .replace('_', '-')
            .ifBlank { DEFAULT_LANGUAGE }
        val key = CacheKey(type, normalizedLanguage)

        cache[key]?.let { entry ->
            if (System.currentTimeMillis() - entry.savedAt < CACHE_TTL_MS) {
                return Result.success(entry.genres)
            }
            cache.remove(key, entry)
        }

        return try {
            val response = when (type) {
                ContentType.MOVIE -> api.movieGenres(normalizedLanguage)
                ContentType.SERIES -> api.seriesGenres(normalizedLanguage)
                ContentType.LIVE -> error("Unsupported content type.")
            }

            val mapped = response.genres
                .filter { it.id > 0 && it.name.isNotBlank() }
                .distinctBy { it.id }
                .map { TmdbGenre(it.id, it.name.trim()) }

            if (mapped.isNotEmpty()) {
                cache[key] = CacheEntry(mapped, System.currentTimeMillis())
            }

            Result.success(mapped)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun clearCache() {
        cache.clear()
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "pl-PL"
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
