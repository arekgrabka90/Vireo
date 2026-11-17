package app.vireo.player

internal enum class NetworkAvailability { UNKNOWN, AVAILABLE, LOST }

internal enum class WatchdogAction { NONE, WAIT_FOR_NETWORK, RETRY_CURRENT, FAILOVER }

/** Pure decision for a stalled buffering state. The timeout is a proposal. */
internal object BufferingWatchdogPolicy {
    const val STALL_TIMEOUT_MS = 10_000L

    fun decide(
        stalledForMs: Long,
        retriesUsed: Int,
        network: NetworkAvailability,
    ): WatchdogAction {
        if (stalledForMs < STALL_TIMEOUT_MS) return WatchdogAction.NONE
        if (network == NetworkAvailability.LOST) return WatchdogAction.WAIT_FOR_NETWORK
        return if (retriesUsed < PlaybackRecoveryPolicy.MAX_CURRENT_SOURCE_RETRIES) {
            WatchdogAction.RETRY_CURRENT
        } else {
            WatchdogAction.FAILOVER
        }
    }
}
