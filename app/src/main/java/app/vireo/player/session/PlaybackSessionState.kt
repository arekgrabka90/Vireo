package app.vireo.player.session

import app.vireo.player.source.PlaybackSource
import java.util.UUID

/**
 * Immutable state snapshot for an independent playback session.
 *
 * This model does not own a player engine and does not manage
 * Android or Compose lifecycle.
 */
data class PlaybackSessionState(
    val sessionId: String,
    val queue: PlaybackQueue,
    val activeSource: PlaybackSource? = null,
) {
    val currentItem
        get() = queue.current

    val hasNext: Boolean
        get() = queue.hasNext

    val hasPrevious: Boolean
        get() = queue.hasPrevious

    companion object {
        fun create(
            queue: PlaybackQueue,
            activeSource: PlaybackSource? = null,
        ): PlaybackSessionState = PlaybackSessionState(
            sessionId = UUID.randomUUID().toString(),
            queue = queue,
            activeSource = activeSource,
        )
    }
}
