package app.vireo.data.xtream

import app.vireo.domain.model.Channel
import app.vireo.domain.model.Movie
import app.vireo.domain.model.Series
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class XtreamStreamingListParserTest {

    private companion object {
        const val SERVER = " tv.example.invalid:8080/ "
        const val BASE = "https://tv.example.invalid:8080"
        const val USER = "user"
        const val PASS = "pass"
        val JSON = Json { ignoreUnknownKeys = true }

        val MOVIE_ROWS = listOf(
            """{"stream_id":1,"name":"A","stream_icon":"http://i/1.jpg","category_id":"5","rating":"7.1","container_extension":"mkv","tmdb_id":"603","imdb_id":"TT0133093"}""",
            """{"stream_id":"2","name":"B"}""",
            """{"stream_id":3}""",
            """{"stream_id":4,"name":"  ","rating":7.5,"tmdb":0,"imdbid":"nope"}""",
            """{"name":"No id"}""",
            """{"stream_id":"","name":"Blank id"}""",
            """{"stream_id":5,"name":"Za\u017c\u00f3\u0142\u0107 \"quote\" ] } [","tmdbid":"12"}""",
            """{"stream_id":6,"name":null,"container_extension":null}""",
            """{"stream_id":7,"name":"Obj","category_id":{"x":1}}""",
        )
        const val MOVIE_ROWS_WITHOUT_ID = 2

        val SERIES_ROWS = listOf(
            """{"series_id":20,"name":"S","cover":"http://i/s.jpg","category_id":"2","rating":"8","tmdb_id":"1399","imdb":"tt0944947"}""",
            """{"series_id":"21"}""",
            """{"name":"nope"}""",
        )

        val CHANNEL_ROWS = listOf(
            """{"stream_id":10,"name":"Chan","stream_icon":"http://i/c.png","category_id":"1","epg_channel_id":"c.pl"}""",
            """{"stream_id":"11"}""",
            """{"name":"x"}""",
        )
    }

    private fun array(rows: List<String>) = rows.joinToString(prefix = "[", postfix = "]", separator = ",")

    private fun parser(max: Long = JsonArrayElementSplitter.DEFAULT_MAX_ELEMENT_BYTES) =
        XtreamStreamingListParser(SERVER, USER, PASS, JSON, max)

    private fun provider(body: String): XtreamProvider {
        val client = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(body.toResponseBody("application/json".toMediaType()))
                        .build()
                },
            )
            .build()
        return XtreamProvider(SERVER, USER, PASS, client, JSON)
    }

    private fun movies(text: String): Pair<List<Movie>, XtreamListParseSummary> {
        val out = ArrayList<Movie>()
        val summary = parser().parseMovies(Buffer().writeUtf8(text)) { out += it }
        return out to summary
    }

    @Test
    fun `movies match the existing provider on the same payload`() {
        val payload = array(MOVIE_ROWS)
        val old = runBlocking { provider(payload).movies(null) }.getOrThrow()
        val (new, summary) = movies(payload)

        assertEquals(old, new)
        assertEquals(MOVIE_ROWS_WITHOUT_ID, summary.skipped)
        assertEquals(new.size, summary.decoded)
        assertEquals(JsonArrayEnd.COMPLETE, summary.end)

        assertEquals(
            Movie(
                id = "1", title = "A", posterUrl = "http://i/1.jpg", categoryId = "5",
                rating = "7.1", streamUrl = "$BASE/movie/$USER/$PASS/1.mkv",
                tmdbId = 603, imdbId = "tt0133093", containerExtension = "mkv",
            ),
            new[0],
        )
        assertEquals(
            Movie(
                id = "2", title = "B", posterUrl = null, categoryId = null, rating = null,
                streamUrl = "$BASE/movie/$USER/$PASS/2.mp4",
            ),
            new[1],
        )
        assertEquals("4", new[3].title)
        assertEquals("7.5", new[3].rating)
        assertEquals(null, new[3].tmdbId)
        assertEquals(null, new[3].imdbId)
    }

    @Test
    fun `series match the existing provider on the same payload`() {
        val payload = array(SERIES_ROWS)
        val old = runBlocking { provider(payload).series(null) }.getOrThrow()
        val out = ArrayList<Series>()
        val summary = parser().parseSeries(Buffer().writeUtf8(payload)) { out += it }

        assertEquals(old, out)
        assertEquals(1, summary.skipped)
        assertEquals(2, summary.decoded)
        assertEquals(
            Series(
                id = "20", title = "S", posterUrl = "http://i/s.jpg", categoryId = "2",
                rating = "8", tmdbId = 1399, imdbId = "tt0944947",
            ),
            out[0],
        )
    }

    @Test
    fun `channels match the existing provider on the same payload`() {
        val payload = array(CHANNEL_ROWS)
        val old = runBlocking { provider(payload).channels(null) }.getOrThrow()
        val out = ArrayList<Channel>()
        val summary = parser().parseChannels(Buffer().writeUtf8(payload)) { out += it }

        assertEquals(old, out)
        assertEquals(1, summary.skipped)
        assertEquals(
            Channel(
                id = "10", name = "Chan", iconUrl = "http://i/c.png", categoryId = "1",
                epgChannelId = "c.pl", streamUrl = "$BASE/live/$USER/$PASS/10.m3u8",
            ),
            out[0],
        )
    }

    @Test
    fun `non-object rows are skipped and counted, the old path fails instead`() {
        val payload = """[{"stream_id":1,"name":"ok"}, 1, null, "x", [1], {"stream_id":2,"name":"ok2"}]"""
        val (items, summary) = movies(payload)
        assertEquals(listOf("1", "2"), items.map { it.id })
        assertEquals(4, summary.skipped)
        assertEquals(JsonArrayEnd.COMPLETE, summary.end)

        // Documented intentional difference: the old path rejects the whole list.
        assertTrue(runBlocking { provider(payload).movies(null) }.isFailure)
    }

    @Test
    fun `malformed element is skipped`() {
        val payload = """[{"stream_id":1,"name":"ok"}, {"stream_id":2,"name":}, {"stream_id":3,"name":"c"}]"""
        val (items, summary) = movies(payload)
        assertEquals(listOf("1", "3"), items.map { it.id })
        assertEquals(1, summary.skipped)
        assertTrue(summary.isComplete)
    }

    @Test
    fun `truncated stream is reported and is not complete`() {
        val payload = array((0 until 100).map { """{"stream_id":$it,"name":"Film $it"}""" })
        val (items, summary) = movies(payload.substring(0, 1_000))
        assertEquals(JsonArrayEnd.TRUNCATED, summary.end)
        assertFalse(summary.isComplete)
        assertTrue("decoded=${items.size}", items.size in 1 until 100)
        assertEquals(0, summary.skipped)
    }

    @Test
    fun `empty array, empty input and object are distinguishable`() {
        assertEquals(XtreamListParseSummary(0, 0, JsonArrayEnd.COMPLETE), movies("[]").second)
        assertEquals(XtreamListParseSummary(0, 0, JsonArrayEnd.EMPTY), movies("").second)
        assertEquals(
            XtreamListParseSummary(0, 0, JsonArrayEnd.NOT_ARRAY),
            movies("""{"user_info":{"auth":0}}""").second,
        )
    }

    @Test
    fun `oversized element ends the read as OVERSIZED`() {
        val payload = """[{"stream_id":1,"name":"a"},{"stream_id":2,"name":"${"x".repeat(500)}"}]"""
        val out = ArrayList<Movie>()
        val summary = parser(max = 64L).parseMovies(Buffer().writeUtf8(payload)) { out += it }
        assertEquals(1, out.size)
        assertEquals(JsonArrayEnd.OVERSIZED, summary.end)
        assertFalse(summary.isComplete)
    }

    @Test
    fun `items are delivered before the whole input is consumed`() {
        class Stop : RuntimeException()

        val buffer = Buffer().writeUtf8(
            array((0 until 5_000).map { """{"stream_id":$it,"name":"Film $it"}""" }),
        )
        val total = buffer.size
        try {
            parser().parseMovies(buffer) { throw Stop() }
            fail("callback should have been invoked")
        } catch (_: Stop) {
        }
        assertTrue("unread=${buffer.size} of $total", buffer.size > total / 2)
    }
}
