package app.vireo.player.engine

import app.vireo.player.normalizePlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayerEngineStateTest {

    @Test
    fun `progress clamps negative values and duration overflow`() {
        val progress = normalizePlayerProgress(
            positionMs = 15_000L,
            durationMs = 10_000L,
            bufferedPositionMs = 12_000L,
            isSeekable = true,
        )

        assertEquals(10_000L, progress.positionMs)
        assertEquals(10_000L, progress.durationMs)
        assertEquals(10_000L, progress.bufferedPositionMs)
        assertEquals(true, progress.isSeekable)
    }

    @Test
    fun `unknown duration disables seeking`() {
        val progress = normalizePlayerProgress(
            positionMs = -10L,
            durationMs = 0L,
            bufferedPositionMs = -20L,
            isSeekable = true,
        )

        assertEquals(0L, progress.positionMs)
        assertEquals(0L, progress.durationMs)
        assertEquals(0L, progress.bufferedPositionMs)
        assertFalse(progress.isSeekable)
    }

    @Test
    fun `engine state starts idle`() {
        val state = PlayerEngineState()

        assertEquals(
            app.vireo.player.PlayerTransportState.IDLE,
            state.transportState,
        )
        assertFalse(state.isPlaying)
        assertEquals(null, state.error)
    }
}
