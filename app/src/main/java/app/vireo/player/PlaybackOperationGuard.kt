package app.vireo.player

/**
 * Invalidates asynchronous playback operations when a newer operation starts.
 */
internal class PlaybackOperationGuard {
    private var generation = 0L

    @Synchronized
    fun begin(): Long {
        generation += 1L
        return generation
    }

    @Synchronized
    fun invalidate(): Long = begin()

    @Synchronized
    fun isCurrent(operation: Long): Boolean =
        operation == generation
}
