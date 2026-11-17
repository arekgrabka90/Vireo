package app.vireo.player

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneCoordinatorTest {

    @Test
    fun `sequences increase and report the superseded tune`() {
        val c = TuneCoordinator<String>()
        val a = c.begin("A")
        val b = c.begin("B")
        assertNull(a.supersededSequence)
        assertEquals(a.sequence, b.supersededSequence)
        assertTrue(b.sequence > a.sequence)
    }

    @Test
    fun `last request wins and late results are ignored`() {
        val c = TuneCoordinator<String>()
        val a = c.begin("A")
        val b = c.begin("B")
        assertFalse(c.onSourceResolved(a.sequence))
        assertNull(c.committed())
        assertTrue(c.onSourceResolved(b.sequence))
        assertEquals("B", c.committed())
        assertNull(c.pending())
    }

    @Test
    fun `resolution failure keeps the previous channel`() {
        val c = TuneCoordinator<String>()
        assertTrue(c.onSourceResolved(c.begin("A").sequence))
        val b = c.begin("B")
        assertTrue(c.onSourceResolutionFailed(b.sequence))
        assertEquals("A", c.committed())
        assertNull(c.pending())
    }

    @Test
    fun `stale failure of a superseded tune is ignored`() {
        val c = TuneCoordinator<String>()
        val a = c.begin("A")
        val b = c.begin("B")
        assertFalse(c.onSourceResolutionFailed(a.sequence))
        assertEquals("B", c.pending())
        assertTrue(c.isCurrent(b.sequence))
    }

    @Test
    fun `start failure clears only the matching committed tune`() {
        val c = TuneCoordinator<String>()
        val a = c.begin("A")
        assertTrue(c.onSourceResolved(a.sequence))
        assertFalse(c.onStartFailed(a.sequence + 1))
        assertEquals("A", c.committed())
        assertTrue(c.onStartFailed(a.sequence))
        assertNull(c.committed())
    }

    @Test
    fun `cancel all returns the in flight sequence and clears state`() {
        val c = TuneCoordinator<String>()
        c.onSourceResolved(c.begin("A").sequence)
        val b = c.begin("B")
        assertEquals(b.sequence, c.cancelAll())
        assertNull(c.pending())
        assertNull(c.committed())
        assertNull(c.cancelAll())
    }

    @Test
    fun `random interleavings never commit a stale tune`() {
        val random = Random(7)
        val c = TuneCoordinator<Int>()
        val begun = mutableListOf<TuneTicket<Int>>()
        var latest: Long? = null
        repeat(2_000) { step ->
            when (random.nextInt(4)) {
                0 -> {
                    val t = c.begin(step)
                    begun += t
                    latest = t.sequence
                }
                1 -> if (begun.isNotEmpty()) {
                    val t = begun[random.nextInt(begun.size)]
                    val ok = c.onSourceResolved(t.sequence)
                    assertEquals(t.sequence == latest, ok)
                    if (ok) {
                        latest = null
                        assertEquals(t.target, c.committed())
                    }
                }
                2 -> if (begun.isNotEmpty()) {
                    val t = begun[random.nextInt(begun.size)]
                    val ok = c.onSourceResolutionFailed(t.sequence)
                    assertEquals(t.sequence == latest, ok)
                    if (ok) latest = null
                }
                else -> assertEquals(latest != null, c.pending() != null)
            }
        }
    }

    @Test
    fun `single press after idle is immediate`() {
        val c = TuneCoalescer()
        assertEquals(0L, c.delayFor(1_000L))
        assertEquals(0L, c.delayFor(10_000L))
    }

    @Test
    fun `rapid presses inside the window are delayed`() {
        val c = TuneCoalescer()
        assertEquals(0L, c.delayFor(1_000L))
        assertEquals(TuneCoalescer.DEFAULT_DELAY_MS, c.delayFor(1_200L))
        assertEquals(TuneCoalescer.DEFAULT_DELAY_MS, c.delayFor(1_500L))
        assertEquals(0L, c.delayFor(2_000L))
    }

    @Test
    fun `repeated key events are always delayed`() {
        val c = TuneCoalescer()
        assertEquals(
            TuneCoalescer.DEFAULT_DELAY_MS,
            c.delayFor(1_000L, repeatCount = 1),
        )
    }

    @Test
    fun `clock going backwards is not treated as a burst`() {
        val c = TuneCoalescer()
        assertEquals(0L, c.delayFor(5_000L))
        assertEquals(0L, c.delayFor(4_000L))
    }
}
