package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbCatalogRepository @Inject constructor(
    private val api: TmdbApi,
) {
    private enum class CatalogKind { POPULAR, TRENDING }

    private data class CacheKey(
        val type: ContentType,
        val page: Int,
        val language: String,
        val kind: CatalogKind,
    )

    private data class CacheEntry(
        val value: TmdbCatalogPage,
        val savedAt: Long,
    )

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()

    suspend fun popularMovies(
        page: Int = 1,
        language: String = DEFAULT_LANGUAGE,
    ): Result<TmdbCatalogPage> =
        load(ContentType.MOVIE, page, language, CatalogKind.POPULAR)

    suspend fun popularSeries(
        page: Int = 1,
        language: String = DEFAULT_LANGUAGE,
    ): Result<TmdbCatalogPage> =
        load(ContentType.SERIES, page, language, CatalogKind.POPULAR)

    suspend fun popular(
        type: ContentType,
        page: Int = 1,
        language: String = DEFAULT_LANGUAGE,
    ): Result<TmdbCatalogPage> {
        if (type == ContentType.LIVE) {
            return Result.failure(
                IllegalArgumentException("TMDB catalog supports movies and series, not Live TV.")
            )
        }
        return load(type, page, language, CatalogKind.POPULAR)
    }

    suspend fun trending(
        type: ContentType,
        page: Int = 1,
        language: String = DEFAULT_LANGUAGE,
    ): Result<TmdbCatalogPage> {
        if (type == ContentType.LIVE) {
            return Result.failure(
                IllegalArgumentException("TMDB catalog supports movies and series, not Live TV.")
            )
        }
        return load(type, page, language, CatalogKind.TRENDING)
    }

    suspend fun search(
        query: String,
        type: ContentType,
        page: Int = 1,
        language: String = DEFAULT_LANGUAGE,
    ): Result<TmdbCatalogPage> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Search query cannot be blank.")
            )
        }
        if (type == ContentType.LIVE) {
            return Result.failure(
                IllegalArgumentException("TMDB search supports movies and series only.")
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

        return try {
            val response = when (type) {
                ContentType.MOVIE ->
                    api.searchMovies(normalizedQuery, page, normalizedLanguage)
                ContentType.SERIES ->
                    api.searchSeries(normalizedQuery, page, normalizedLanguage)
                ContentType.LIVE -> error("Unsupported TMDB content type.")
            }

            Result.success(
                TmdbCatalogPage(
                    items = response.results.mapNotNull {
                        TmdbCatalogMapper.map(it, type)
                    },
                    page = response.page,
                    totalPages = response.totalPages,
                    totalResults = response.totalResults,
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun searchMulti(
        query: String,
        page: Int = 1,
        language: String = DEFAULT_LANGUAGE,
    ): Result<TmdbMultiSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Search query cannot be blank.")
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

        return try {
            val response = api.searchMulti(
                normalizedQuery,
                page,
                normalizedLanguage,
            )

            val mapped = response.results.mapNotNull(TmdbMultiSearchMapper::map)
            Result.success(
                TmdbMultiSearchResult(
                    media = mapped.filterIsInstance<TmdbMultiSearchMapped.Media>()
                        .map { it.item }
                        .distinctBy { "${it.type.name}:${it.tmdbId}" },
                    people = mapped.filterIsInstance<TmdbMultiSearchMapped.Person>()
                        .map { it.item }
                        .distinctBy { it.tmdbId }
                        .sortedByDescending { it.popularity }
                        .take(10),
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun clearCache() {
        cache.clear()
    }

    private suspend fun load(
        type: ContentType,
        page: Int,
        language: String,
        kind: CatalogKind,
    ): Result<TmdbCatalogPage> {
        if (page !in 1..500) {
            return Result.failure(
                IllegalArgumentException("TMDB page must be between 1 and 500.")
            )
        }

        val normalizedLanguage = language.trim()
            .replace('_', '-')
            .ifBlank { DEFAULT_LANGUAGE }

        val key = CacheKey(type, page, normalizedLanguage, kind)
        val now = System.currentTimeMillis()

        cache[key]?.let { entry ->
            if (now - entry.savedAt < CACHE_TTL_MS) {
                return Result.success(entry.value)
            }
            cache.remove(key, entry)
        }

        return try {
            val response = when (kind) {
                CatalogKind.POPULAR -> when (type) {
                    ContentType.MOVIE -> api.popularMovies(page, normalizedLanguage)
                    ContentType.SERIES -> api.popularSeries(page, normalizedLanguage)
                    ContentType.LIVE -> error("Unsupported TMDB content type.")
                }
                CatalogKind.TRENDING -> when (type) {
                    ContentType.MOVIE -> api.trendingMovies(page, normalizedLanguage)
                    ContentType.SERIES -> api.trendingSeries(page, normalizedLanguage)
                    ContentType.LIVE -> error("Unsupported TMDB content type.")
                }
            }

            val result = TmdbCatalogPage(
                items = response.results.mapNotNull { TmdbCatalogMapper.map(it, type) },
                page = response.page,
                totalPages = response.totalPages,
                totalResults = response.totalResults,
            )

            // Nie przechowujemy pustych odpowiedzi:
            // kolejna próba może wtedy ponownie pobrać katalog.
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

    private fun trimCache() {
        if (cache.size <= MAX_CACHE_ENTRIES) return

        val oldest = cache.entries.minByOrNull { it.value.savedAt } ?: return
        cache.remove(oldest.key, oldest.value)
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "pl-PL"
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 48
    }
}
