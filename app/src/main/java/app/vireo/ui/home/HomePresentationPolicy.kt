package app.vireo.ui.home

import app.vireo.domain.model.ContentType

internal fun shouldUseExpandedHomeHero(
    screenWidthDp: Int,
    screenHeightDp: Int,
    smallestScreenWidthDp: Int,
    isTelevision: Boolean,
): Boolean =
    isTelevision ||
        smallestScreenWidthDp >= 600 ||
        (screenWidthDp >= 600 && screenHeightDp >= 600)

internal fun homeHeroKey(type: ContentType, tmdbId: Int): String =
    "${type.name}:$tmdbId"

internal fun resolveHomeHeroIndex(
    keys: List<String>,
    activeKey: String?,
): Int {
    if (keys.isEmpty()) return -1
    val index = activeKey?.let(keys::indexOf) ?: -1
    return if (index >= 0) index else 0
}

internal fun homeProgressFraction(
    positionMs: Long,
    durationMs: Long,
): Float? {
    if (durationMs <= 0L) return null
    return (positionMs.toDouble() / durationMs.toDouble())
        .toFloat()
        .coerceIn(0f, 1f)
}

internal fun formatHomeRemainingDuration(durationMs: Long): String {
    val safeDuration = durationMs.coerceAtLeast(0L)
    val totalMinutes = safeDuration / 60_000L +
        if (safeDuration % 60_000L == 0L) 0L else 1L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    return when {
        hours > 0L && minutes > 0L -> "$hours godz. $minutes min"
        hours > 0L -> "$hours godz."
        else -> "$minutes min"
    }
}
