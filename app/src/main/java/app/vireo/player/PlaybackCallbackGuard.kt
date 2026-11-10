package app.vireo.player

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

    fun isCurrentPlaybackState(
        callbackState: Int,
        currentPlayerState: Int,
    ): Boolean = callbackState == currentPlayerState

    fun isCurrentBooleanValue(
        callbackValue: Boolean,
        currentPlayerValue: Boolean,
    ): Boolean = callbackValue == currentPlayerValue
}
