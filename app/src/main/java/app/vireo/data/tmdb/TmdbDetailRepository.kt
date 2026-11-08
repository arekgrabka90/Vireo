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

    private val cache = ConcurrentHashMap<Key, Entry>()

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
            val detail = TmdbDetailMapper.map(dto, type)
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

    private fun trimCache() {
        if (cache.size <= MAX_CACHE_ENTRIES) return
        val oldest = cache.entries.minByOrNull { it.value.savedAt } ?: return
        cache.remove(oldest.key, oldest.value)
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "pl-PL"
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 100
    }
}
