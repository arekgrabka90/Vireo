package app.vireo.data.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupCrashGuardTest {

    private class FakeStore : StartMarkerStore {
        override var pendingStart: Boolean = false
        override var unstableStarts: Int = 0
    }

    @Test
    fun firstStartIsNotSuspect() {
        val guard = StartupCrashGuard(FakeStore())
        assertFalse(guard.onStart())
    }

    @Test
    fun cleanSessionsNeverTrigger() {
        val guard = StartupCrashGuard(FakeStore())
        repeat(10) {
            assertFalse(guard.onStart())
            guard.markStable()
        }
    }

    @Test
    fun threeConsecutiveUnstableStartsTriggerOnFourth() {
        val guard = StartupCrashGuard(FakeStore())
        assertFalse(guard.onStart())
        assertFalse(guard.onStart())
        assertFalse(guard.onStart())
        assertTrue(guard.onStart())
    }

    @Test
    fun markStableResetsTheCounter() {
        val store = FakeStore()
        val guard = StartupCrashGuard(store)
        guard.onStart()
        guard.onStart()
        guard.onStart()
        guard.markStable()

        assertEquals(0, store.unstableStarts)
        assertFalse(store.pendingStart)
        assertFalse(guard.onStart())
    }

    @Test
    fun resetClearsCounterButKeepsCurrentStartPending() {
        val store = FakeStore()
        val guard = StartupCrashGuard(store)
        repeat(4) { guard.onStart() }

        guard.reset()

        assertEquals(0, store.unstableStarts)
        assertTrue(store.pendingStart)
        assertFalse(guard.onStart())
    }

    @Test
    fun thresholdIsConfigurable() {
        val guard = StartupCrashGuard(FakeStore(), threshold = 1)
        assertFalse(guard.onStart())
        assertTrue(guard.onStart())
    }
}
