package app.vireo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePresentationPolicyTest {

    @Test
    fun heroKeepsActiveKeyAfterReordering() {
        assertEquals(
            0,
            resolveHomeHeroIndex(listOf("b", "a", "c"), "b"),
        )
        assertEquals(
            1,
            resolveHomeHeroIndex(listOf("b", "a", "c"), "a"),
        )
    }

    @Test
    fun heroFallsBackWhenActiveKeyIsMissing() {
        assertEquals(0, resolveHomeHeroIndex(listOf("a", "b"), "missing"))
        assertEquals(0, resolveHomeHeroIndex(listOf("a", "b"), null))
        assertEquals(-1, resolveHomeHeroIndex(emptyList(), "a"))
    }

    @Test
    fun progressIsNormalizedAndClamped() {
        assertEquals(0.5f, homeProgressFraction(50L, 100L)!!, 0.0001f)
        assertEquals(0f, homeProgressFraction(-10L, 100L)!!, 0.0001f)
        assertEquals(1f, homeProgressFraction(150L, 100L)!!, 0.0001f)
    }

    @Test
    fun unknownDurationHasNoProgress() {
        assertNull(homeProgressFraction(10L, 0L))
        assertNull(homeProgressFraction(10L, -1L))
    }

    @Test
    fun remainingDurationIsFormatted() {
        assertEquals("1 godz. 30 min", formatHomeRemainingDuration(5_400_000L))
        assertEquals("2 godz.", formatHomeRemainingDuration(7_200_000L))
        assertEquals("0 min", formatHomeRemainingDuration(0L))
        assertEquals("0 min", formatHomeRemainingDuration(-1L))
    }
}
