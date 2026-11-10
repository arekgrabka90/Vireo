package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbDiscoveryRepository @Inject constructor(
    private val api: TmdbApi,
) {
    private data class CacheKey(
        val category: TmdbDiscoveryCategory,
        val type: ContentType,
        val filters: TmdbDiscoveryFilters,
        val page: Int,
        val language: String,
        val date: LocalDate,
    )

    private data class CacheEntry(
        val page: TmdbCatalogPage,
        val savedAt: Long,
    )

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()

    suspend fun discover(
        category: TmdbDiscoveryCategory,
        type: ContentType,
        filters: TmdbDiscoveryFilters = TmdbDiscoveryFilters(),
        page: Int = 1,
        language: String = DEFAULT_LANGUAGE,
        today: LocalDate = LocalDate.now(),
    ): Result<TmdbCatalogPage> {
        if (type != ContentType.MOVIE && type != ContentType.SERIES) {
            return Result.failure(
                IllegalArgumentException("Discovery supports movies and series only.")
            )
        }
        if (page !in 1..500) {
            return Result.failure(
                IllegalArgumentException("TMDB page must be between 1 and 500.")
            )
        }

        val normalizedLanguage = language.trim()
            .replace('_', '-')
            .ifBlank { DEFAULT_LANGUAGE }

        val params = try {
            TmdbDiscoveryQueryBuilder.build(category, type, filters, today)
        } catch (error: Exception) {
            return Result.failure(error)
        }

        val key = CacheKey(
            category = category,
            type = type,
            filters = filters.copy(genreIds = filters.genreIds.distinct().sorted()),
            page = page,
            language = normalizedLanguage,
            date = today,
        )

        val now = System.currentTimeMillis()
        cache[key]?.let { entry ->
            if (now - entry.savedAt < CACHE_TTL_MS) {
                return Result.success(entry.page)
            }
            cache.remove(key, entry)
        }

        return try {
            val response = when (category) {
                TmdbDiscoveryCategory.TRENDING_WEEK ->
                    when (type) {
                        ContentType.MOVIE -> api.trendingMoviesWeek(page, normalizedLanguage)
                        ContentType.SERIES -> api.trendingSeriesWeek(page, normalizedLanguage)
                        ContentType.LIVE -> error("Unsupported content type.")
                    }
                TmdbDiscoveryCategory.TOP_RATED ->
                    when (type) {
                        ContentType.MOVIE -> api.topRatedMovies(page, normalizedLanguage)
                        ContentType.SERIES -> api.topRatedSeries(page, normalizedLanguage)
                        ContentType.LIVE -> error("Unsupported content type.")
                    }
                else ->
                    when (type) {
                        ContentType.MOVIE ->
                            api.discoverMovies(params, page, normalizedLanguage)
                        ContentType.SERIES ->
                            api.discoverSeries(params, page, normalizedLanguage)
                        ContentType.LIVE -> error("Unsupported content type.")
                    }
            }

            val result = TmdbCatalogPage(
                items = response.results.mapNotNull {
                    TmdbCatalogMapper.map(it, type)
                },
                page = response.page,
                totalPages = response.totalPages,
                totalResults = response.totalResults,
            )

            if (result.items.isNotEmpty()) {
                cache[key] = CacheEntry(result, System.currentTimeMillis())
                trimCache()
            }

            Result.success(result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun clearCache() {
        cache.clear()
    }

    private fun trimCache() {
        while (cache.size > MAX_CACHE_ENTRIES) {
            val oldest = cache.entries.minByOrNull { it.value.savedAt } ?: return
            cache.remove(oldest.key, oldest.value)
        }
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "pl-PL"
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 48
    }
}
