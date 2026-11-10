package app.vireo.data.xtream

import app.vireo.domain.model.EpgProgram
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import android.util.Base64
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object XtreamEpgParser {

    fun parse(streamId: String, payload: JsonElement): List<EpgProgram> {
        val entries = when (payload) {
            is JsonArray -> payload
            is JsonObject -> payload["epg_listings"] as? JsonArray
                ?: payload["data"] as? JsonArray
                ?: throw IOException("Invalid Xtream EPG response")
            else -> throw IOException("Invalid Xtream EPG response")
        }

        return entries.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val title = decode(obj.text("title") ?: obj.text("name"))
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val start = timestamp(
                obj.text("start_timestamp"),
                obj.text("start"),
                obj.text("start_time"),
            ) ?: return@mapNotNull null

            val end = timestamp(
                obj.text("stop_timestamp"),
                obj.text("end_timestamp"),
                obj.text("end"),
                obj.text("stop"),
                obj.text("end_time"),
            ) ?: return@mapNotNull null

            if (end <= start) return@mapNotNull null

            EpgProgram(
                channelId = streamId,
                title = title,
                description = decode(
                    obj.text("description")
                        ?: obj.text("descr")
                        ?: obj.text("plot")
                )?.takeIf { it.isNotBlank() },
                startTimeMs = start,
                endTimeMs = end,
                category = decode(
                    obj.text("category") ?: obj.text("genre")
                )?.takeIf { it.isNotBlank() },
                iconUrl = obj.text("icon")?.takeIf { it.startsWith("http") },
            )
        }.distinctBy { it.startTimeMs to it.title }
            .sortedBy { it.startTimeMs }
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }

    private fun timestamp(vararg values: String?): Long? {
        for (value in values) {
            val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            val numeric = raw.toLongOrNull()
            if (numeric != null) {
                return if (numeric >= 100_000_000_000L) numeric
                else numeric * 1000L
            }

            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
            )
            for (pattern in patterns) {
                try {
                    val parser = SimpleDateFormat(pattern, Locale.US).apply {
                        isLenient = false
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val parsed: Date? = parser.parse(raw)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                    // Try the next known date representation.
                }
            }
        }
        return null
    }

    private fun decode(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.length < 12 || raw.length % 4 != 0) return raw
        if (!raw.matches(Regex("[A-Za-z0-9+/]+={0,2}"))) return raw

        return try {
            val decoded = String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8)
            if (decoded.isNotBlank() &&
                decoded.none { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }
            ) decoded.trim() else raw
        } catch (_: Exception) {
            raw
        }
    }
}
