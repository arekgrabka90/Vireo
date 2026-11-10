package app.vireo.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackOperationGuardTest {

    @Test
    fun `new operation invalidates previous generation`() {
        val guard = PlaybackOperationGuard()
        val first = guard.begin()
        val second = guard.begin()

        assertFalse(guard.isCurrent(first))
        assertTrue(guard.isCurrent(second))
    }

    @Test
    fun `manual invalidation makes pending operation stale`() {
        val guard = PlaybackOperationGuard()
        val operation = guard.begin()

        guard.invalidate()

        assertFalse(guard.isCurrent(operation))
    }

    @Test
    fun `latest operation remains current until invalidated`() {
        val guard = PlaybackOperationGuard()
        val operation = guard.begin()

        assertTrue(guard.isCurrent(operation))
        assertTrue(guard.isCurrent(operation))
    }
}
