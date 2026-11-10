package app.vireo.player

import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCallbackGuardTest {

    @Test
    fun `accepts only the exact active playback error instance`() {
        val activeError = Any()

        assertTrue(
            PlaybackCallbackGuard.isCurrentError(activeError, activeError)
        )
    }

    @Test
    fun `rejects an error from a previous playback instance`() {
        val oldError = Any()
        val activeError = Any()

        assertFalse(
            PlaybackCallbackGuard.isCurrentError(oldError, activeError)
        )
    }

    @Test
    fun `rejects an error when player has no active error`() {
        assertFalse(
            PlaybackCallbackGuard.isCurrentError(Any(), null)
        )
    }

    @Test
    fun `accepts only the active media item id`() {
        assertTrue(
            PlaybackCallbackGuard.isCurrentMediaItem("source-2", "source-2")
        )
    }

    @Test
    fun `rejects a callback from a previous media item`() {
        assertFalse(
            PlaybackCallbackGuard.isCurrentMediaItem("source-1", "source-2")
        )
    }

    @Test
    fun `rejects missing media item identity`() {
        assertFalse(
            PlaybackCallbackGuard.isCurrentMediaItem(null, "source-2")
        )
        assertFalse(
            PlaybackCallbackGuard.isCurrentMediaItem("source-2", null)
        )
    }

    @OptIn(UnstableApi::class)
    @Test
    fun `rejects an event without a valid timeline window`() {
        assertFalse(
            PlaybackCallbackGuard.isCurrentMediaItem(
                Timeline.EMPTY,
                0,
                "source-2",
            )
        )

        assertFalse(
            PlaybackCallbackGuard.isCurrentMediaItem(
                Timeline.EMPTY,
                -1,
                "source-2",
            )
        )
    }

    @Test
    fun `accepts only the current playback state`() {
        assertTrue(
            PlaybackCallbackGuard.isCurrentPlaybackState(3, 3)
        )
        assertFalse(
            PlaybackCallbackGuard.isCurrentPlaybackState(3, 2)
        )
    }

    @Test
    fun `accepts only the current boolean player value`() {
        assertTrue(
            PlaybackCallbackGuard.isCurrentBooleanValue(true, true)
        )
        assertFalse(
            PlaybackCallbackGuard.isCurrentBooleanValue(true, false)
        )
    }
}
