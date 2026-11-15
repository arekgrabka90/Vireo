package app.vireo.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinAttemptPolicyTest {

    @Test
    fun noLockBeforeFreeAttemptsAreUsed() {
        for (failures in 0 until PinAttemptPolicy.FREE_ATTEMPTS) {
            assertEquals(0L, PinAttemptPolicy.lockDurationMs(failures))
        }
    }

    @Test
    fun firstLockAfterFiveFailuresIsThirtySeconds() {
        assertEquals(30_000L, PinAttemptPolicy.lockDurationMs(5))
    }

    @Test
    fun lockDoublesWithEachFurtherFailure() {
        assertEquals(60_000L, PinAttemptPolicy.lockDurationMs(6))
        assertEquals(120_000L, PinAttemptPolicy.lockDurationMs(7))
        assertEquals(240_000L, PinAttemptPolicy.lockDurationMs(8))
        assertEquals(480_000L, PinAttemptPolicy.lockDurationMs(9))
    }

    @Test
    fun lockIsCappedAtFifteenMinutes() {
        assertEquals(PinAttemptPolicy.MAX_LOCK_MS, PinAttemptPolicy.lockDurationMs(10))
        assertEquals(PinAttemptPolicy.MAX_LOCK_MS, PinAttemptPolicy.lockDurationMs(11))
        assertEquals(PinAttemptPolicy.MAX_LOCK_MS, PinAttemptPolicy.lockDurationMs(1_000))
    }

    @Test
    fun remainingLockIsZeroWithoutFailures() {
        assertEquals(0L, PinAttemptPolicy.remainingLockMs(0, 0L, 10_000L))
        assertEquals(0L, PinAttemptPolicy.remainingLockMs(4, 5_000L, 5_000L))
    }

    @Test
    fun remainingLockCountsDownFromLastFailure() {
        assertEquals(30_000L, PinAttemptPolicy.remainingLockMs(5, 1_000L, 1_000L))
        assertEquals(20_000L, PinAttemptPolicy.remainingLockMs(5, 1_000L, 11_000L))
        assertEquals(1L, PinAttemptPolicy.remainingLockMs(5, 1_000L, 30_999L))
    }

    @Test
    fun remainingLockIsZeroAfterExpiry() {
        assertEquals(0L, PinAttemptPolicy.remainingLockMs(5, 1_000L, 31_000L))
        assertEquals(0L, PinAttemptPolicy.remainingLockMs(5, 1_000L, 500_000L))
    }

    @Test
    fun clockMovedBackReturnsFullLockDuration() {
        assertEquals(30_000L, PinAttemptPolicy.remainingLockMs(5, 50_000L, 10_000L))
        assertEquals(60_000L, PinAttemptPolicy.remainingLockMs(6, 50_000L, 10_000L))
    }

    @Test
    fun attemptsLeftBeforeLockNeverGoesNegative() {
        assertEquals(5, PinAttemptPolicy.attemptsLeftBeforeLock(0))
        assertEquals(2, PinAttemptPolicy.attemptsLeftBeforeLock(3))
        assertEquals(0, PinAttemptPolicy.attemptsLeftBeforeLock(5))
        assertEquals(0, PinAttemptPolicy.attemptsLeftBeforeLock(9))
    }
}
