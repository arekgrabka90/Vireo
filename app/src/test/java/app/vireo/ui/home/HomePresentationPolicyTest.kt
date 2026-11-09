package app.vireo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePresentationPolicyTest {

    @Test
    fun phoneUsesSingleHeroInPortraitAndLandscape() {
        assertFalse(shouldUseExpandedHomeHero(393, 852, 393, false))
        assertFalse(shouldUseExpandedHomeHero(852, 393, 393, false))
    }

    @Test
    fun tabletUsesExpandedHeroInPortraitAndLandscape() {
        assertTrue(shouldUseExpandedHomeHero(800, 1280, 800, false))
        assertTrue(shouldUseExpandedHomeHero(1280, 800, 800, false))
    }

    @Test
    fun largeWindowUsesExpandedHeroWhenBothDimensionsAreTabletSized() {
        assertTrue(shouldUseExpandedHomeHero(700, 720, 560, false))
    }

    @Test
    fun televisionAlwaysUsesExpandedHero() {
        assertTrue(shouldUseExpandedHomeHero(320, 240, 320, true))
    }

    @Test
    fun heroKeyIncludesMediaTypeAndTmdbId() {
        assertEquals("MOVIE:42", homeHeroKey(app.vireo.domain.model.ContentType.MOVIE, 42))
        assertEquals("SERIES:42", homeHeroKey(app.vireo.domain.model.ContentType.SERIES, 42))
    }

    @Test
    fun heroKeepsActiveKeyAfterReordering() {
        assertEquals(0, resolveHomeHeroIndex(listOf("b", "a", "c"), "b"))
        assertEquals(1, resolveHomeHeroIndex(listOf("b", "a", "c"), "a"))
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
