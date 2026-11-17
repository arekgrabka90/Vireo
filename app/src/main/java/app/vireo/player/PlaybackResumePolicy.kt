package app.vireo.player

internal enum class ResumeAction { RESUME_IN_PLACE, RESET_SOURCE }

/** docs/specs/playback-host.md section 5. VOD always keeps its position. */
internal object PlaybackResumePolicy {
    const val LIVE_RESET_AFTER_PAUSE_MS = 120_000L
    const val LIVE_BACKGROUND_RELEASE_MS = 60_000L

    fun onResume(
        isLive: Boolean,
        pausedForMs: Long,
        sourceReleased: Boolean = false,
    ): ResumeAction =
        if (isLive && (sourceReleased || pausedForMs.coerceAtLeast(0L) > LIVE_RESET_AFTER_PAUSE_MS)) {
            ResumeAction.RESET_SOURCE
        } else {
            ResumeAction.RESUME_IN_PLACE
        }

    fun shouldReleaseLiveInBackground(
        isLive: Boolean,
        backgroundedForMs: Long,
    ): Boolean =
        isLive && backgroundedForMs.coerceAtLeast(0L) >= LIVE_BACKGROUND_RELEASE_MS
}
