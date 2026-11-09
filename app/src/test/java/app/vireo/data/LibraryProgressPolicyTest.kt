package app.vireo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryProgressPolicyTest {

    @Test
    fun `position is clamped to media duration`() {
        val result = normalizeLibraryProgress(
            positionMs = 120_000L,
            durationMs = 100_000L,
            previousDurationMs = 0L,
            alreadyWatched = false,
        )

        assertEquals(100_000L, result.positionMs)
        assertEquals(100_000L, result.durationMs)
        assertEquals(true, result.watched)
    }

    @Test
    fun `95 percent marks item as watched`() {
        val result = normalizeLibraryProgress(
            positionMs = 9_500L,
            durationMs = 10_000L,
            previousDurationMs = 0L,
            alreadyWatched = false,
        )

        assertEquals(0L, result.positionMs)
        assertEquals(true, result.watched)
    }

    @Test
    fun `progress below 95 percent remains resumable`() {
        val result = normalizeLibraryProgress(
            positionMs = 9_499L,
            durationMs = 10_000L,
            previousDurationMs = 0L,
            alreadyWatched = false,
        )

        assertEquals(9_499L, result.positionMs)
        assertEquals(false, result.watched)
    }

    @Test
    fun `missing duration preserves previous duration`() {
        val result = normalizeLibraryProgress(
            positionMs = 5_000L,
            durationMs = 0L,
            previousDurationMs = 10_000L,
            alreadyWatched = false,
        )

        assertEquals(5_000L, result.positionMs)
        assertEquals(10_000L, result.durationMs)
        assertEquals(false, result.watched)
    }

    @Test
    fun `previous watched status cannot be accidentally cleared`() {
        val result = normalizeLibraryProgress(
            positionMs = 2_000L,
            durationMs = 10_000L,
            previousDurationMs = 10_000L,
            alreadyWatched = true,
        )

        assertEquals(0L, result.positionMs)
        assertEquals(true, result.watched)
    }

    @Test
    fun `negative values are normalized safely`() {
        val result = normalizeLibraryProgress(
            positionMs = -100L,
            durationMs = -1L,
            previousDurationMs = 10_000L,
            alreadyWatched = false,
        )

        assertEquals(0L, result.positionMs)
        assertEquals(10_000L, result.durationMs)
        assertEquals(false, result.watched)
    }
}
