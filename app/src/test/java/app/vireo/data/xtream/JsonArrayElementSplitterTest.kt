package app.vireo.data.xtream

import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonArrayElementSplitterTest {

    private fun splitSource(
        source: BufferedSource,
        max: Long = JsonArrayElementSplitter.DEFAULT_MAX_ELEMENT_BYTES,
    ): Pair<List<String>, JsonArrayEnd> {
        val out = ArrayList<String>()
        val end = JsonArrayElementSplitter(source, max).forEachElement { out += it }
        return out to end
    }

    private fun split(text: String, max: Long = JsonArrayElementSplitter.DEFAULT_MAX_ELEMENT_BYTES) =
        splitSource(Buffer().writeUtf8(text), max)

    @Test
    fun `splits simple objects`() {
        val (items, end) = split("""[{"a":1},{"b":2}]""")
        assertEquals(listOf("""{"a":1}""", """{"b":2}"""), items)
        assertEquals(JsonArrayEnd.COMPLETE, end)
    }

    @Test
    fun `structural characters inside strings do not split`() {
        val element = """{"n":"x ] } [ { , \" y"}"""
        val (items, end) = split("[$element]")
        assertEquals(listOf(element), items)
        assertEquals(JsonArrayEnd.COMPLETE, end)
    }

    @Test
    fun `escaped backslash before a quote closes the string`() {
        val (items, end) = split("""[{"n":"a\\"},{"n":"b"}]""")
        assertEquals(listOf("""{"n":"a\\"}""", """{"n":"b"}"""), items)
        assertEquals(JsonArrayEnd.COMPLETE, end)
    }

    @Test
    fun `nested arrays and objects stay in one element`() {
        val (items, end) = split("""[{"a":[1,[2,{"b":3}]]}, {"c":{}}]""")
        assertEquals(listOf("""{"a":[1,[2,{"b":3}]]}""", """{"c":{}}"""), items)
        assertEquals(JsonArrayEnd.COMPLETE, end)
    }

    @Test
    fun `scalars are returned as elements`() {
        val (items, end) = split("""[1,null,"x",true]""")
        assertEquals(listOf("1", "null", "\"x\"", "true"), items)
        assertEquals(JsonArrayEnd.COMPLETE, end)
    }

    @Test
    fun `empty arrays`() {
        assertEquals(emptyList<String>() to JsonArrayEnd.COMPLETE, split("[]"))
        assertEquals(emptyList<String>() to JsonArrayEnd.COMPLETE, split("[ \n ]"))
    }

    @Test
    fun `empty input and whitespace only`() {
        assertEquals(emptyList<String>() to JsonArrayEnd.EMPTY, split(""))
        assertEquals(emptyList<String>() to JsonArrayEnd.EMPTY, split(" \n\t "))
    }

    @Test
    fun `object instead of array`() {
        assertEquals(
            emptyList<String>() to JsonArrayEnd.NOT_ARRAY,
            split("""{"user_info":{"auth":0}}"""),
        )
    }

    @Test
    fun `pretty printed payload with BOM`() {
        val (items, end) = split("\uFEFF[\n  {\"a\":1},\n  {\"a\":2}\n]")
        assertEquals(listOf("""{"a":1}""", """{"a":2}"""), items)
        assertEquals(JsonArrayEnd.COMPLETE, end)
    }

    @Test
    fun `truncated input keeps completed elements and reports TRUNCATED`() {
        assertEquals(
            listOf("""{"a":1}""") to JsonArrayEnd.TRUNCATED,
            split("""[{"a":1},{"b":"""),
        )
        assertEquals(listOf("""{"a":1}""") to JsonArrayEnd.TRUNCATED, split("""[{"a":1},"""))
        assertEquals(listOf("""{"a":1}""") to JsonArrayEnd.TRUNCATED, split("""[{"a":1}"""))
    }

    @Test
    fun `oversized element is rejected without reading the rest`() {
        val buffer = Buffer().writeUtf8(
            "[{\"a\":1},{\"k\":\"" + "x".repeat(200) + "\"}]",
        )
        val (items, end) = splitSource(buffer, max = 16L)
        assertEquals(listOf("""{"a":1}"""), items)
        assertEquals(JsonArrayEnd.OVERSIZED, end)
        assertTrue("rest unread=${buffer.size}", buffer.size > 150L)
    }

    @Test
    fun `trailing data after the array is ignored`() {
        assertEquals(listOf("1") to JsonArrayEnd.COMPLETE, split("[1] garbage"))
    }

    @Test
    fun `multi-byte UTF-8 survives`() {
        val element = """{"n":"Zażółć gęślą jaźń 🎬"}"""
        assertEquals(listOf(element) to JsonArrayEnd.COMPLETE, split("[$element]"))
    }

    @Test
    fun `one byte per read gives the same result`() {
        val text = """[{"a":"é"},{"a":2}]"""
        val data = Buffer().writeUtf8(text)
        val source = object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (data.exhausted()) return -1L
                return data.read(sink, minOf(1L, byteCount))
            }

            override fun timeout() = Timeout.NONE

            override fun close() {}
        }.buffer()
        assertEquals(
            listOf("""{"a":"é"}""", """{"a":2}""") to JsonArrayEnd.COMPLETE,
            splitSource(source),
        )
    }

    @Test
    fun `many elements`() {
        val count = 20_000
        val text = (0 until count).joinToString(prefix = "[", postfix = "]", separator = ",") {
            """{"stream_id":$it,"name":"Film $it"}"""
        }
        val (items, end) = split(text)
        assertEquals(count, items.size)
        assertEquals(JsonArrayEnd.COMPLETE, end)
    }
}
