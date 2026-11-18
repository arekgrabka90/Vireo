package app.vireo.data.xtream

import okio.Buffer
import okio.BufferedSource

internal enum class JsonArrayEnd { COMPLETE, TRUNCATED, NOT_ARRAY, EMPTY, OVERSIZED }

/**
 * Splits a top-level JSON array into raw element texts without loading the
 * whole payload. Blocking; run on an IO dispatcher. Quoted strings and
 * escapes are respected, so brackets inside strings never split an element.
 * Every end other than COMPLETE must be treated by the caller as a failed read.
 */
internal class JsonArrayElementSplitter(
    private val source: BufferedSource,
    private val maxElementBytes: Long = DEFAULT_MAX_ELEMENT_BYTES,
) {
    init {
        require(maxElementBytes > 0L) { "maxElementBytes must be positive" }
    }

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

    fun forEachElement(onElement: (String) -> Unit): JsonArrayEnd {
        var c = skipWs()
        if (c == 0xEF) {
            read()
            read()
            c = skipWs()
        }
        if (c == -1) return JsonArrayEnd.EMPTY
        if (c != '['.code) return JsonArrayEnd.NOT_ARRAY

        val element = Buffer()
        while (true) {
            c = skipWs()
            if (c == -1) return JsonArrayEnd.TRUNCATED
            if (c == ']'.code) return JsonArrayEnd.COMPLETE
            if (c == ','.code) continue

            element.clear()
            var depth = 0
            var inString = false
            var escaped = false
            var cur = c
            scan@ while (true) {
                if (cur == -1) return JsonArrayEnd.TRUNCATED
                if (element.size >= maxElementBytes) return JsonArrayEnd.OVERSIZED
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

    companion object {
        const val DEFAULT_MAX_ELEMENT_BYTES: Long = 1L shl 20
        private const val NONE = -2
    }
}
