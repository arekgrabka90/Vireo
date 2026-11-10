package app.vireo.player.engine

import app.vireo.player.PlayerProgressState
import app.vireo.player.PlayerTransportState
import app.vireo.player.source.PlaybackSource
import kotlinx.coroutines.flow.StateFlow

/**
 * Engine-independent playback error.
 *
 * Messages are sanitized before being exposed to observers.
 */
data class PlayerEngineError(
    val code: Int,
    val message: String,
)

/**
 * Observable state owned by the playback engine.
 */
data class PlayerEngineState(
    val transportState: PlayerTransportState = PlayerTransportState.IDLE,
    val isPlaying: Boolean = false,
    val progress: PlayerProgressState = PlayerProgressState(),
    val error: PlayerEngineError? = null,
)

/**
 * Playback engine contract.
 *
 * This interface deliberately exposes neither Media3 nor Compose types.
 */
interface PlayerEngine {
    val state: StateFlow<PlayerEngineState>

    fun setSource(
        source: PlaybackSource,
        startPositionMs: Long = 0L,
    )

    fun prepare()

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun release()
}
