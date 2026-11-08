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
    suspend fun popularMovies(
        page: Int,
        language: String,
    ): TmdbPageDto = getPopular("movie/popular", page, language)

    suspend fun popularSeries(
        page: Int,
        language: String,
    ): TmdbPageDto = getPopular("tv/popular", page, language)

    private suspend fun getPopular(
        endpoint: String,
        page: Int,
        language: String,
    ): TmdbPageDto {
        require(page in 1..500) { "TMDB page must be between 1 and 500." }

        val apiKey = BuildConfig.TMDB_API_KEY.trim()
        check(apiKey.isNotEmpty()) {
            "TMDB API key is not configured. Add TMDB_API_KEY to Gradle properties."
        }

        val normalizedLanguage = language.trim()
            .replace('_', '-')
            .ifBlank { DEFAULT_LANGUAGE }

        val url = "$BASE_URL/$endpoint".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("language", normalizedLanguage)
            .addQueryParameter("page", page.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        val body = execute(request)
        return json.decodeFromString<TmdbPageDto>(body)
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
