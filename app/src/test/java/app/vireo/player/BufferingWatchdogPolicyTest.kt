package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Test

class BufferingWatchdogPolicyTest {
    private val timeout = BufferingWatchdogPolicy.STALL_TIMEOUT_MS
    private val maxRetries = PlaybackRecoveryPolicy.MAX_CURRENT_SOURCE_RETRIES

    @Test
    fun `short stalls do nothing`() {
        NetworkAvailability.values().forEach { network ->
            assertEquals(
                WatchdogAction.NONE,
                BufferingWatchdogPolicy.decide(timeout - 1L, 0, network),
            )
        }
    }

    @Test
    fun `stall retries current source until the budget is used then fails over`() {
        for (used in 0 until maxRetries) {
            assertEquals(
                WatchdogAction.RETRY_CURRENT,
                BufferingWatchdogPolicy.decide(timeout, used, NetworkAvailability.AVAILABLE),
            )
        }
        assertEquals(
            WatchdogAction.FAILOVER,
            BufferingWatchdogPolicy.decide(timeout, maxRetries, NetworkAvailability.AVAILABLE),
        )
    }

    @Test
    fun `lost network waits instead of burning retries`() {
        assertEquals(
            WatchdogAction.WAIT_FOR_NETWORK,
            BufferingWatchdogPolicy.decide(timeout * 5, maxRetries, NetworkAvailability.LOST),
        )
    }

    @Test
    fun `unknown network is treated like available`() {
        assertEquals(
            WatchdogAction.RETRY_CURRENT,
            BufferingWatchdogPolicy.decide(timeout, 0, NetworkAvailability.UNKNOWN),
        )
    }
}
