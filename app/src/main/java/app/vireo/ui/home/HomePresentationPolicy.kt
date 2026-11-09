package app.vireo.ui.home

import app.vireo.domain.model.ContentType
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbDetail
import java.util.Locale

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

/** A focused card is not opened until the user confirms it while it is active. */
internal fun shouldOpenHomeHeroOnClick(isActive: Boolean): Boolean = isActive

/** Active cards use the wide backdrop; inactive cards preserve the complete poster. */
internal fun homeHeroArtworkUrl(
    backdropUrl: String?,
    posterUrl: String?,
    selected: Boolean,
): String? {
    val backdrop = backdropUrl?.takeIf { it.isNotBlank() }
    val poster = posterUrl?.takeIf { it.isNotBlank() }
    return if (selected) backdrop ?: poster else poster ?: backdrop
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


/** Poster height decreases with distance from the active Hero. */
internal fun homeHeroPosterHeightFraction(distanceFromActive: Int): Float =
    when (distanceFromActive) {
        1 -> 0.76f
        2 -> 0.60f
        else -> 1f
    }

/** Poster width decreases with distance from the active Hero. */
internal fun homeHeroPosterWidthFraction(distanceFromActive: Int): Float =
    when (distanceFromActive) {
        1 -> 0.205f
        2 -> 0.17f
        else -> 0.56f
    }

/** Metadata values are returned in the visual order used by the Hero. */
internal fun homeHeroMetadataValues(
    item: TmdbCatalogItem,
    detail: TmdbDetail?,
): List<String> {
    val year = detail?.year?.toString()
        ?: item.year?.toString()
        ?: item.releaseDate?.take(4)?.takeIf { it.length == 4 }

    val runtime = detail?.runtimeMinutes
        ?.takeIf { it > 0 }
        ?.let { "$it min" }

    val genres = detail?.genres
        .orEmpty()
        .asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(2)
        .joinToString(" • ")
        .takeIf(String::isNotBlank)

    val rating = (detail?.rating ?: item.rating)
        ?.takeIf { it > 0.0 }
        ?.let { String.format(Locale.US, "%.1f", it) }

    return listOfNotNull(year, runtime, genres, rating)
}
