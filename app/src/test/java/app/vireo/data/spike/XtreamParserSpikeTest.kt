package app.vireo.data.spike

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeToSequence
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * E2.0 spike (docs/specs/xtream-sync.md section 3). Test-only code: compares
 * variant A (decodeToSequence) with variant B (okio array splitter + per-element
 * parse). Measurements go to build/e2-spike/report.txt and stdout.
 * No production code is touched.
 */
class XtreamParserSpikeTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun resetReport() {
            runCatching {
                reportFile.parentFile?.mkdirs()
                reportFile.writeText("")
            }
        }
    }

    @Test
    fun `clean payload gives identical items in A and B`() {
        val a = runA(stream(3_000), keepItems = true)
        val b = runB(stream(3_000), keepItems = true)
        describe("equivalence A", a)
        describe("equivalence B", b)
        val expected = (0 until 3_000).map { (100_000L + it) to expectedName(it) }
        assertEquals("COMPLETE", b.end)
        assertEquals(expected, b.items)
        assertEquals(expected, a.items)
    }

    @Test
    fun `first item needs a bounded number of bytes`() {
        val b = runB(stream(150_000), stopAfter = 1)
        val a = runA(stream(150_000), stopAfter = 1)
        describe("first item B", b)
        describe("first item A", a)
        assertEquals(1, b.decoded)
        assertTrue("B bytes=${b.bytesAtFirst}", b.bytesAtFirst in 1L..65_536L)
        assertEquals(1, a.decoded)
    }

    @Test
    fun `150k items - B keeps a small live heap, A is measured`() {
        val total = 150_000
        val b = runB(stream(total))
        describe("scale B", b)
        assertEquals(total, b.decoded)
        assertEquals(0, b.skipped)
        assertEquals("COMPLETE", b.end)
        assertTrue("B live heap ${b.peakLiveMb} MB", b.peakLiveMb < 64.0)
        val a = runA(stream(total))
        describe("scale A", a)
        assertEquals(total, a.decoded)
    }

    @Test
    fun `empty array`() {
        val a = runA(bytes("[]"))
        val b = runB(bytes("[]"))
        describe("empty array A", a)
        describe("empty array B", b)
        assertEquals(0, a.decoded)
        assertEquals(null, a.error)
        assertEquals(0, b.decoded)
        assertEquals("COMPLETE", b.end)
    }

    @Test
    fun `empty input`() {
        val a = runA(bytes(""))
        val b = runB(bytes(""))
        describe("empty input A", a)
        describe("empty input B", b)
        assertEquals("EMPTY", b.end)
        assertEquals(0, b.decoded)
    }

    @Test
    fun `object instead of array`() {
        val text = "{\"user_info\":{\"auth\":0}}"
        val a = runA(bytes(text))
        val b = runB(bytes(text))
        describe("object A", a)
        describe("object B", b)
        assertEquals(0, a.decoded)
        assertNotNull(a.error)
        assertEquals("NOT_ARRAY", b.end)
        assertEquals(0, b.decoded)
    }

    @Test
    fun `truncated stream`() {
        val a = runA(stream(1_000, cutAtByte = 200_000L))
        val b = runB(stream(1_000, cutAtByte = 200_000L))
        describe("truncated A", a)
        describe("truncated B", b)
        assertNotNull(a.error)
        assertEquals("TRUNCATED", b.end)
        assertTrue("B decoded=${b.decoded}", b.decoded in 1 until 1_000)
        assertEquals(0, b.skipped)
    }

    @Test
    fun `bad element aborts A but is skipped by B`() {
        val a = runA(stream(100, badAt = setOf(40)))
        val b = runB(stream(100, badAt = setOf(40)))
        describe("bad element A", a)
        describe("bad element B", b)
        assertEquals(40, a.decoded)
        assertNotNull(a.error)
        assertEquals("COMPLETE", b.end)
        assertEquals(99, b.decoded)
        assertEquals(1, b.skipped)
    }

    @Test
    fun `scalar null and nested elements are skipped by B`() {
        val text = "[1,null,\"x\",{\"stream_id\":7,\"name\":\"ok\"},[1,[2]]]"
        val a = runA(bytes(text))
        val b = runB(bytes(text), keepItems = true)
        describe("scalars A", a)
        describe("scalars B", b)
        assertNotNull(a.error)
        assertEquals("COMPLETE", b.end)
        assertEquals(listOf(7L to "ok"), b.items)
        assertEquals(4, b.skipped)
    }

    @Test
    fun `pretty printed payload with BOM`() {
        val a = runA(stream(10, pretty = true, bom = true))
        val b = runB(stream(10, pretty = true, bom = true))
        describe("pretty BOM A", a)
        describe("pretty BOM B", b)
        assertEquals("COMPLETE", b.end)
        assertEquals(10, b.decoded)
    }

    @Test
    fun `oversized element is rejected without reading the rest`() {
        val head = "[{\"stream_id\":1,\"name\":\"a\"},{".toByteArray(Charsets.UTF_8)
        val tail = "}]".toByteArray(Charsets.UTF_8)
        val filler = ByteArray(3 * 1024 * 1024) { 'x'.code.toByte() }
        val counting = CountingInputStream(ByteArrayInputStream(head + filler + tail))
        val b = runB(counting)
        describe("oversized B", b)
        assertEquals("OVERSIZED", b.end)
        assertEquals(1, b.decoded)
        assertTrue("bytes read=${counting.count}", counting.count < filler.size)
    }
}

// ---------- shared helpers ----------

private const val BATCH = 1_000
private const val SAMPLE_EVERY = 10_000

private val json = Json { ignoreUnknownKeys = true }
private val reportFile = File("build/e2-spike/report.txt")

private fun report(line: String) {
    println("[E2.0-SPIKE] $line")
    runCatching {
        reportFile.parentFile?.mkdirs()
        reportFile.appendText(line + "\n")
    }
}

@Serializable
private data class SpikeVod(
    val name: String,
    @SerialName("stream_id") val streamId: Long,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val rating: String? = null,
)

private data class Outcome(
    val decoded: Int,
    val skipped: Int,
    val end: String,
    val error: String?,
    val bytesAtFirst: Long,
    val peakLiveMb: Double,
    val millis: Long,
    val items: List<Pair<Long, String>>,
)

private fun describe(label: String, o: Outcome) = report(
    String.format(
        Locale.ROOT,
        "%s: decoded=%d skipped=%d end=%s error=%s firstItemAfterBytes=%d peakLiveMb=%.1f timeMs=%d",
        label, o.decoded, o.skipped, o.end, o.error, o.bytesAtFirst, o.peakLiveMb, o.millis,
    ),
)

private val PLOT = "Lorem ipsum \\n dolor sit amet, consectetur. ".repeat(8)

private fun element(i: Int): String =
    "{\"num\":$i,\"name\":\"Film $i \\\"Zażółć\\\" ] } [ { \\\\ gęślą jaźń\"," +
        "\"stream_type\":\"movie\",\"stream_id\":${100_000L + i}," +
        "\"stream_icon\":\"https://img.example.invalid/p/$i.jpg\"," +
        "\"rating\":\"7.1\",\"rating_5based\":3.5,\"added\":\"1700000000\"," +
        "\"category_id\":\"${i % 40}\",\"container_extension\":\"mkv\"," +
        "\"custom_sid\":\"\",\"direct_source\":\"\",\"plot\":\"$PLOT\"}"

private fun badElement(i: Int): String =
    "{\"num\":$i,\"name\":null,\"stream_id\":\"abc\"}"

private fun expectedName(i: Int): String = "Film $i \"Zażółć\" ] } [ { \\ gęślą jaźń"

private class CountingInputStream(private val delegate: InputStream) : InputStream() {
    var count = 0L
        private set

    override fun read(): Int {
        val b = delegate.read()
        if (b >= 0) count++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = delegate.read(b, off, len)
        if (n > 0) count += n
        return n
    }

    override fun close() = delegate.close()
}

private class SyntheticArrayStream(
    count: Int,
    badAt: Set<Int> = emptySet(),
    private val cutAtByte: Long? = null,
    pretty: Boolean = false,
    bom: Boolean = false,
) : InputStream() {
    private val chunks: Iterator<ByteArray> = iterator {
        if (bom) yield(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        yield((if (pretty) "[\n" else "[").toByteArray(Charsets.UTF_8))
        for (i in 0 until count) {
            val separator = if (i == 0) "" else if (pretty) ",\n  " else ","
            val body = if (i in badAt) badElement(i) else element(i)
            yield((separator + body).toByteArray(Charsets.UTF_8))
        }
        yield("]".toByteArray(Charsets.UTF_8))
    }
    private var current = ByteArray(0)
    private var pos = 0
    private var total = 0L

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) == -1) -1 else one[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val limit = cutAtByte
        if (limit != null && total >= limit) return -1
        while (pos >= current.size) {
            if (!chunks.hasNext()) return -1
            current = chunks.next()
            pos = 0
        }
        var n = minOf(len, current.size - pos)
        if (limit != null) n = minOf(n.toLong(), limit - total).toInt()
        System.arraycopy(current, pos, b, off, n)
        pos += n
        total += n
        return n
    }
}

private fun stream(
    count: Int,
    badAt: Set<Int> = emptySet(),
    cutAtByte: Long? = null,
    pretty: Boolean = false,
    bom: Boolean = false,
) = CountingInputStream(SyntheticArrayStream(count, badAt, cutAtByte, pretty, bom))

private fun bytes(text: String) =
    CountingInputStream(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))

private class LiveHeapSampler {
    private val runtime = Runtime.getRuntime()
    private val baseline = used()
    private var peakBytes = 0L
    val peakMb: Double get() = peakBytes / (1024.0 * 1024.0)

    private fun used(): Long {
        System.gc()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    fun sample() {
        peakBytes = maxOf(peakBytes, used() - baseline)
    }
}

// ---------- variant A ----------

@OptIn(ExperimentalSerializationApi::class)
private fun runA(
    counting: CountingInputStream,
    stopAfter: Int? = null,
    keepItems: Boolean = false,
): Outcome {
    val sampler = LiveHeapSampler()
    val items = ArrayList<Pair<Long, String>>()
    val batch = ArrayList<SpikeVod>(BATCH)
    var decoded = 0
    var error: String? = null
    var first = -1L
    val started = System.nanoTime()
    try {
        val sequence = json.decodeToSequence(
            counting,
            SpikeVod.serializer(),
            DecodeSequenceMode.ARRAY_WRAPPED,
        )
        for (item in sequence) {
            if (decoded == 0) first = counting.count
            decoded++
            if (keepItems) items += item.streamId to item.name
            batch += item
            if (batch.size >= BATCH) batch.clear()
            if (decoded % SAMPLE_EVERY == 0) sampler.sample()
            if (stopAfter != null && decoded >= stopAfter) break
        }
    } catch (e: Exception) {
        error = e.javaClass.simpleName
    }
    sampler.sample()
    return Outcome(
        decoded = decoded,
        skipped = 0,
        end = if (error == null) "OK" else "ERROR",
        error = error,
        bytesAtFirst = first,
        peakLiveMb = sampler.peakMb,
        millis = (System.nanoTime() - started) / 1_000_000L,
        items = items,
    )
}

// ---------- variant B ----------

private enum class SplitEnd { COMPLETE, TRUNCATED, NOT_ARRAY, EMPTY, OVERSIZED }

private class StopSignal : RuntimeException()

private class ArrayElementSplitter(
    private val source: BufferedSource,
    private val maxElementBytes: Long = 1L shl 20,
) {
    private var pushed = NONE

    private fun read(): Int {
        if (pushed != NONE) {
            val b = pushed
            pushed = NONE
            return b
        }
        return if (source.exhausted()) -1 else source.readByte().toInt() and 0xFF
    }

    private fun isWs(b: Int) = b == 0x20 || b == 0x09 || b == 0x0A || b == 0x0D

    private fun skipWs(): Int {
        while (true) {
            val b = read()
            if (b == -1 || !isWs(b)) return b
        }
    }

    fun forEachElement(onElement: (String) -> Unit): SplitEnd {
        var c = skipWs()
        if (c == 0xEF) {
            read()
            read()
            c = skipWs()
        }
        if (c == -1) return SplitEnd.EMPTY
        if (c != '['.code) return SplitEnd.NOT_ARRAY

        val element = Buffer()
        while (true) {
            c = skipWs()
            if (c == -1) return SplitEnd.TRUNCATED
            if (c == ']'.code) return SplitEnd.COMPLETE
            if (c == ','.code) continue

            element.clear()
            var depth = 0
            var inString = false
            var escaped = false
            var cur = c
            scan@ while (true) {
                if (cur == -1) return SplitEnd.TRUNCATED
                if (element.size >= maxElementBytes) return SplitEnd.OVERSIZED
                if (inString) {
                    element.writeByte(cur)
                    when {
                        escaped -> escaped = false
                        cur == '\\'.code -> escaped = true
                        cur == '"'.code -> {
                            inString = false
                            if (depth == 0) break@scan
                        }
                    }
                } else {
                    when (cur) {
                        '"'.code -> {
                            inString = true
                            element.writeByte(cur)
                        }
                        '{'.code, '['.code -> {
                            depth++
                            element.writeByte(cur)
                        }
                        '}'.code, ']'.code -> {
                            if (depth == 0) {
                                if (cur == ']'.code) {
                                    pushed = cur
                                    break@scan
                                }
                                element.writeByte(cur)
                            } else {
                                depth--
                                element.writeByte(cur)
                                if (depth == 0) break@scan
                            }
                        }
                        ','.code -> {
                            if (depth == 0) {
                                pushed = cur
                                break@scan
                            }
                            element.writeByte(cur)
                        }
                        else -> {
                            if (depth == 0 && isWs(cur)) break@scan
                            element.writeByte(cur)
                        }
                    }
                }
                cur = read()
            }
            onElement(element.readUtf8())
        }
    }

    private companion object {
        const val NONE = -2
    }
}

private fun mapElement(text: String): Pair<Long, String>? =
    try {
        val obj = json.parseToJsonElement(text) as? JsonObject
        val id = (obj?.get("stream_id") as? JsonPrimitive)?.content?.toLongOrNull()
        val name = (obj?.get("name") as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (id == null || name == null) null else id to name
    } catch (_: Exception) {
        null
    }

private fun runB(
    counting: CountingInputStream,
    stopAfter: Int? = null,
    keepItems: Boolean = false,
): Outcome {
    val sampler = LiveHeapSampler()
    val items = ArrayList<Pair<Long, String>>()
    val batch = ArrayList<Pair<Long, String>>(BATCH)
    var decoded = 0
    var skipped = 0
    var first = -1L
    var end = "STOPPED"
    val started = System.nanoTime()
    try {
        end = ArrayElementSplitter(counting.source().buffer()).forEachElement { text ->
            val item = mapElement(text)
            if (item == null) {
                skipped++
                return@forEachElement
            }
            if (decoded == 0) first = counting.count
            decoded++
            if (keepItems) items += item
            batch += item
            if (batch.size >= BATCH) batch.clear()
            if (decoded % SAMPLE_EVERY == 0) sampler.sample()
            if (stopAfter != null && decoded >= stopAfter) throw StopSignal()
        }.name
    } catch (_: StopSignal) {
    }
    sampler.sample()
    return Outcome(
        decoded = decoded,
        skipped = skipped,
        end = end,
        error = null,
        bytesAtFirst = first,
        peakLiveMb = sampler.peakMb,
        millis = (System.nanoTime() - started) / 1_000_000L,
        items = items,
    )
}
