package app.vireo.player.hud

import app.vireo.domain.model.EpgProgram
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** True only for an absolute http(s) URL. Anything else is treated as "no logo". */
internal fun isUsableLogoUrl(url: String?): Boolean {
    val value = url?.trim().orEmpty()
    if (value.isEmpty()) return false
    return runCatching {
        val uri = URI(value)
        (uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

/** Fraction of the programme elapsed at [nowMs], or null when the range is invalid or [nowMs] is outside it. */
internal fun EpgProgram.progressAt(nowMs: Long): Float? {
    if (!hasValidTimeRange || !contains(nowMs)) return null
    return ((nowMs - startTimeMs).toDouble() / (endTimeMs - startTimeMs).toDouble())
        .toFloat()
        .coerceIn(0f, 1f)
}

/** Whole minutes left (rounded up), or null when the programme is not running at [nowMs]. */
internal fun EpgProgram.remainingMinutesAt(nowMs: Long): Int? {
    if (!hasValidTimeRange || !contains(nowMs)) return null
    return (((endTimeMs - nowMs) + 59_999L) / 60_000L).toInt()
}

/** "2026 · Drama · 120 min · ★ 8.1" built only from parts that exist; null when nothing exists. */
internal fun joinMetadata(
    year: String?,
    genre: String?,
    duration: String?,
    rating: String?,
): String? {
    val parts = listOfNotNull(
        year?.trim()?.takeIf { it.isNotEmpty() },
        genre?.trim()?.takeIf { it.isNotEmpty() },
        duration?.trim()?.takeIf { it.isNotEmpty() },
        rating?.trim()?.takeIf { it.isNotEmpty() }?.let { "★ $it" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

/** "S01 · E02 · Title" using only the parts that exist; null when nothing exists. */
internal fun formatEpisodeLabel(season: Int?, episode: Int?, title: String?): String? {
    val parts = listOfNotNull(
        season?.let { "S${it.toString().padStart(2, '0')}" },
        episode?.let { "E${it.toString().padStart(2, '0')}" },
        title?.trim()?.takeIf { it.isNotEmpty() },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

internal fun formatPlayerTime(valueMs: Long): String {
    val totalSeconds = valueMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/**
 * Wall-clock formatting with injected patterns so it is testable on the JVM.
 * Not thread-safe (SimpleDateFormat): use on the main thread only.
 */
internal class HudTimeFormat(
    clockPattern: String,
    timePattern: String,
    locale: Locale,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    private val clockFormat = SimpleDateFormat(clockPattern, locale).apply { this.timeZone = timeZone }
    private val timeFormat = SimpleDateFormat(timePattern, locale).apply { this.timeZone = timeZone }

    fun clock(epochMs: Long): String = clockFormat.format(Date(epochMs))
    fun time(epochMs: Long): String = timeFormat.format(Date(epochMs))
    fun range(startMs: Long, endMs: Long): String = "${time(startMs)} – ${time(endMs)}"
}
