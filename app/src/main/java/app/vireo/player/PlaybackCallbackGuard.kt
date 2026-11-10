package app.vireo.player

import androidx.media3.common.Timeline

/**
 * Guards callbacks against the currently active Media3 source.
 */
internal object PlaybackCallbackGuard {

    fun <T : Any> isCurrentError(
        callbackError: T,
        currentPlayerError: T?,
    ): Boolean = callbackError === currentPlayerError

    fun isCurrentMediaItem(
        callbackMediaId: String?,
        currentMediaId: String?,
    ): Boolean =
        !callbackMediaId.isNullOrBlank() &&
            callbackMediaId == currentMediaId

    fun isCurrentMediaItem(
        eventTimeline: Timeline,
        eventWindowIndex: Int,
        currentMediaId: String?,
    ): Boolean {
        if (
            eventTimeline.isEmpty ||
            eventWindowIndex !in 0 until eventTimeline.windowCount
        ) {
            return false
        }

        val callbackMediaId = eventTimeline
            .getWindow(eventWindowIndex, Timeline.Window())
            .mediaItem
            .mediaId

        return isCurrentMediaItem(callbackMediaId, currentMediaId)
    }

    fun isCurrentPlaybackState(
        callbackState: Int,
        currentPlayerState: Int,
    ): Boolean = callbackState == currentPlayerState

    fun isCurrentBooleanValue(
        callbackValue: Boolean,
        currentPlayerValue: Boolean,
    ): Boolean = callbackValue == currentPlayerValue
}
