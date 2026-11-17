package app.vireo.player

internal data class TuneTicket<T>(
    val sequence: Long,
    val target: T,
    /** Sequence of the in-flight tune replaced by this one; the caller cancels it. */
    val supersededSequence: Long?,
)

/**
 * Single-flight, last-request-wins tune bookkeeping
 * (docs/specs/live-tv-hub-and-guide.md section 9). Pure state machine:
 * no coroutines, no host access. Late results of superseded tunes are rejected.
 */
internal class TuneCoordinator<T : Any> {
    private var lastSequence = 0L
    private var pendingSequence: Long? = null
    private var pendingTarget: T? = null
    private var committedSequence: Long? = null
    private var committedTarget: T? = null

    @Synchronized
    fun begin(target: T): TuneTicket<T> {
        val superseded = pendingSequence
        lastSequence += 1L
        pendingSequence = lastSequence
        pendingTarget = target
        return TuneTicket(lastSequence, target, superseded)
    }

    @Synchronized
    fun isCurrent(ticketSequence: Long): Boolean = pendingSequence == ticketSequence

    /** True: the caller may now stop the old stream and start the new source. */
    @Synchronized
    fun onSourceResolved(ticketSequence: Long): Boolean {
        if (pendingSequence != ticketSequence) return false
        committedSequence = ticketSequence
        committedTarget = pendingTarget
        pendingSequence = null
        pendingTarget = null
        return true
    }

    /** Resolution failed: the previously committed channel stays untouched. */
    @Synchronized
    fun onSourceResolutionFailed(ticketSequence: Long): Boolean {
        if (pendingSequence != ticketSequence) return false
        pendingSequence = null
        pendingTarget = null
        return true
    }

    /** Starting the committed source failed: nothing is playing any more. */
    @Synchronized
    fun onStartFailed(ticketSequence: Long): Boolean {
        if (committedSequence != ticketSequence) return false
        committedSequence = null
        committedTarget = null
        return true
    }

    /** Returns the in-flight sequence (if any) that the caller must cancel. */
    @Synchronized
    fun cancelAll(): Long? {
        val inFlight = pendingSequence
        pendingSequence = null
        pendingTarget = null
        committedSequence = null
        committedTarget = null
        return inFlight
    }

    @Synchronized
    fun pending(): T? = pendingTarget

    @Synchronized
    fun committed(): T? = committedTarget
}

/**
 * Coalescing of rapid repeated actions (held CH+/CH-). A single press after
 * an idle gap runs immediately. Default values are proposals pending approval.
 */
internal class TuneCoalescer(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val delayMs: Long = DEFAULT_DELAY_MS,
) {
    private var lastRequestAtMs: Long? = null

    @Synchronized
    fun delayFor(nowMs: Long, repeatCount: Int = 0): Long {
        val last = lastRequestAtMs
        lastRequestAtMs = nowMs
        val burst = repeatCount > 0 ||
            (last != null && (nowMs - last) in 0L until windowMs)
        return if (burst) delayMs else 0L
    }

    @Synchronized
    fun reset() {
        lastRequestAtMs = null
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 400L
        const val DEFAULT_DELAY_MS = 250L
    }
}
