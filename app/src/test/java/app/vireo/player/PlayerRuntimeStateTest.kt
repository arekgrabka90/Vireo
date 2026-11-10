package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRuntimeStateTest {

    @Test
    fun `negative progress values are normalized`() {
        val result = normalizePlayerProgress(
            positionMs = -10L,
            durationMs = -1L,
            bufferedPositionMs = -20L,
            isSeekable = true,
        )

        assertEquals(0L, result.positionMs)
        assertEquals(0L, result.durationMs)
        assertEquals(0L, result.bufferedPositionMs)
        assertFalse(result.isSeekable)
    }

    @Test
    fun `position and buffer are clamped to duration`() {
        val result = normalizePlayerProgress(
            positionMs = 120_000L,
            durationMs = 100_000L,
            bufferedPositionMs = 150_000L,
            isSeekable = true,
        )

        assertEquals(100_000L, result.positionMs)
        assertEquals(100_000L, result.durationMs)
        assertEquals(100_000L, result.bufferedPositionMs)
        assertTrue(result.isSeekable)
    }

    @Test
    fun `unknown duration does not enable seeking`() {
        val result = normalizePlayerProgress(
            positionMs = 5_000L,
            durationMs = 0L,
            bufferedPositionMs = 8_000L,
            isSeekable = true,
        )

        assertEquals(5_000L, result.positionMs)
        assertEquals(0L, result.durationMs)
        assertEquals(8_000L, result.bufferedPositionMs)
        assertFalse(result.isSeekable)
    }

    @Test
    fun `runtime state exposes error status`() {
        val state = PlayerRuntimeState(errorMessage = "Playback failed")

        assertTrue(state.hasError)
    }

    @Test
    fun `blank error is not treated as an error`() {
        val state = PlayerRuntimeState(errorMessage = " ")

        assertFalse(state.hasError)
    }
}
