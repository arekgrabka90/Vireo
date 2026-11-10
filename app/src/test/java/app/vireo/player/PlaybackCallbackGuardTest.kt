package app.vireo.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class PlaybackCallbackGuardTest {

    @Test
    fun `accepts only the exact active playback error instance`() {
        val activeError = PlaybackException(
            "active",
            null,
            PlaybackException.ERROR_CODE_TIMEOUT,
        )

        assertTrue(
            PlaybackCallbackGuard.isCurrentError(activeError, activeError)
        )
    }

    @Test
    fun `rejects an error from a previous playback instance`() {
        val oldError = PlaybackException(
            "old",
            null,
            PlaybackException.ERROR_CODE_TIMEOUT,
        )
        val activeError = PlaybackException(
            "active",
            null,
            PlaybackException.ERROR_CODE_TIMEOUT,
        )

        assertFalse(
            PlaybackCallbackGuard.isCurrentError(oldError, activeError)
        )
    }

    @Test
    fun `rejects an error when player has no active error`() {
        val oldError = PlaybackException(
            "old",
            null,
            PlaybackException.ERROR_CODE_TIMEOUT,
        )

        assertFalse(
            PlaybackCallbackGuard.isCurrentError(oldError, null)
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
