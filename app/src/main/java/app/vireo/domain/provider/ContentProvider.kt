package app.vireo.domain.provider

import app.vireo.domain.model.AccountInfo
import app.vireo.domain.model.Category
import app.vireo.domain.model.Channel
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Movie
import app.vireo.domain.model.MovieDetails
import app.vireo.domain.model.Series
import app.vireo.domain.model.SeriesDetails
import app.vireo.domain.provider.EpgProvider

/** Wspólny interfejs źródeł treści (Xtream teraz, addony Stremio później). */
interface ContentProvider : EpgProvider {
    suspend fun authenticate(): Result<AccountInfo>
    suspend fun categories(type: ContentType): Result<List<Category>>
    suspend fun channels(categoryId: String? = null): Result<List<Channel>>
    suspend fun movies(categoryId: String? = null): Result<List<Movie>>
    suspend fun series(categoryId: String? = null): Result<List<Series>>
    suspend fun movieDetails(id: String): Result<MovieDetails>
    suspend fun seriesDetails(id: String): Result<SeriesDetails>
    fun liveUrl(id: String): String
}
