package app.vireo.data.tmdb

import app.vireo.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class TmdbApi @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    suspend fun popularMovies(page: Int, language: String): TmdbPageDto =
        getPopular("movie/popular", page, language)

    suspend fun popularSeries(page: Int, language: String): TmdbPageDto =
        getPopular("tv/popular", page, language)

    suspend fun trendingMovies(page: Int, language: String): TmdbPageDto =
        getPopular("trending/movie/day", page, language)

    suspend fun trendingSeries(page: Int, language: String): TmdbPageDto =
        getPopular("trending/tv/day", page, language)

    suspend fun searchMovies(query: String, page: Int, language: String): TmdbPageDto =
        search("search/movie", query, page, language)

    suspend fun searchSeries(query: String, page: Int, language: String): TmdbPageDto =
        search("search/tv", query, page, language)

    private suspend fun search(
        endpoint: String,
        query: String,
        page: Int,
        language: String,
    ): TmdbPageDto {
        require(query.isNotBlank()) { "Search query cannot be blank." }
        require(page in 1..500) { "TMDB page must be between 1 and 500." }
        val request = request(endpoint, language) {
            addQueryParameter("query", query.trim())
            addQueryParameter("page", page.toString())
            addQueryParameter("include_adult", "false")
        }
        return json.decodeFromString(execute(request))
    }

    suspend fun movieDetails(id: Int, language: String): TmdbDetailDto =
        getDetails("movie/$id", id, language)

    suspend fun seriesDetails(id: Int, language: String): TmdbDetailDto =
        getDetails("tv/$id", id, language)

    suspend fun images(type: String, id: Int): TmdbImagesDto {
        require(type == "movie" || type == "tv") {
            "Unsupported TMDB image media type."
        }
        require(id > 0) { "TMDB ID must be positive." }
        val request = request("$type/$id/images", null) { }
        return json.decodeFromString(execute(request))
    }

    suspend fun videos(type: String, id: Int, language: String): TmdbVideosDto {
        require(type == "movie" || type == "tv") {
            "Unsupported TMDB video media type."
        }
        require(id > 0) { "TMDB ID must be positive." }
        val request = request("$type/$id/videos", language) { }
        return json.decodeFromString(execute(request))
    }

    suspend fun seasonDetails(
        seriesId: Int,
        seasonNumber: Int,
        language: String,
    ): TmdbSeasonDetailsDto {
        require(seriesId > 0) { "TMDB series ID must be positive." }
        require(seasonNumber >= 0) { "TMDB season number cannot be negative." }
        val request = request("tv/$seriesId/season/$seasonNumber", language) { }
        return json.decodeFromString(execute(request))
    }

    private suspend fun getPopular(
        endpoint: String,
        page: Int,
        language: String,
    ): TmdbPageDto {
        require(page in 1..500) { "TMDB page must be between 1 and 500." }
        val request = request(endpoint, language) {
            addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(execute(request))
    }

    private suspend fun getDetails(
        endpoint: String,
        id: Int,
        language: String,
    ): TmdbDetailDto {
        require(id > 0) { "TMDB ID must be positive." }
        val request = request(endpoint, language) {
            addQueryParameter("append_to_response", "credits,external_ids")
        }
        return json.decodeFromString(execute(request))
    }

    private fun request(
        endpoint: String,
        language: String?,
        configure: okhttp3.HttpUrl.Builder.() -> Unit,
    ): Request {
        val apiKey = BuildConfig.TMDB_API_KEY.trim()
        check(apiKey.isNotEmpty()) {
            "TMDB API key is not configured. Add TMDB_API_KEY to Gradle properties."
        }

        val normalizedLanguage = language?.trim()
            ?.replace('_', '-')
            ?.ifBlank { DEFAULT_LANGUAGE }

        val builder = "$BASE_URL/$endpoint".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)

        if (!normalizedLanguage.isNullOrBlank()) {
            builder.addQueryParameter("language", normalizedLanguage)
        }

        builder.configure()

        return Request.Builder()
            .url(builder.build())
            .get()
            .header("Accept", "application/json")
            .build()
    }

    private suspend fun execute(request: Request): String =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IOException("TMDB request failed (HTTP ${it.code}).")
                                )
                            }
                            return
                        }

                        val body = try {
                            it.body?.string().orEmpty()
                        } catch (error: Exception) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(error)
                            }
                            return
                        }

                        if (body.isBlank()) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IOException("TMDB returned an empty response.")
                                )
                            }
                        } else if (continuation.isActive) {
                            continuation.resume(body)
                        }
                    }
                }
            })
        }

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3"
        private const val DEFAULT_LANGUAGE = "pl-PL"
    }
}
