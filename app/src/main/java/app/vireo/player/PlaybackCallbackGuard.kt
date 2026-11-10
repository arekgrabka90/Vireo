package app.vireo.player

import androidx.media3.common.PlaybackException

/**
 * Rejects Media3 callbacks whose payload no longer matches current player state.
 */
internal object PlaybackCallbackGuard {

    fun isCurrentError(
        callbackError: PlaybackException,
        currentPlayerError: PlaybackException?,
    ): Boolean = callbackError === currentPlayerError

    fun isCurrentPlaybackState(
        callbackState: Int,
        currentPlayerState: Int,
    ): Boolean = callbackState == currentPlayerState

    fun isCurrentBooleanValue(
        callbackValue: Boolean,
        currentPlayerValue: Boolean,
    ): Boolean = callbackValue == currentPlayerValue
}
