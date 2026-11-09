package app.vireo.data

internal data class NormalizedLibraryProgress(
    val positionMs: Long,
    val durationMs: Long,
    val watched: Boolean,
)

internal fun normalizeLibraryProgress(
    positionMs: Long,
    durationMs: Long,
    previousDurationMs: Long,
    alreadyWatched: Boolean,
): NormalizedLibraryProgress {
    val effectiveDuration = (
        durationMs.takeIf { it > 0L }
            ?: previousDurationMs.coerceAtLeast(0L)
        )

    val nonNegativePosition = positionMs.coerceAtLeast(0L)
    val normalizedPosition = if (effectiveDuration > 0L) {
        nonNegativePosition.coerceAtMost(effectiveDuration)
    } else {
        nonNegativePosition
    }

    val completed = effectiveDuration > 0L &&
        normalizedPosition.toDouble() / effectiveDuration.toDouble() >= 0.95

    val watched = alreadyWatched || completed

    return NormalizedLibraryProgress(
        positionMs = if (watched) 0L else normalizedPosition,
        durationMs = effectiveDuration,
        watched = watched,
    )
}
