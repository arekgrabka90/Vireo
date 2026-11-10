package app.vireo.player.session

import app.vireo.player.PlayItem

/**
 * Immutable queue snapshot for one playback session.
 *
 * Navigation methods return new queue instances and never mutate
 * the original queue.
 */
data class PlaybackQueue private constructor(
    val items: List<PlayItem>,
    val currentIndex: Int,
) {
    val isEmpty: Boolean
        get() = items.isEmpty()

    val size: Int
        get() = items.size

    val current: PlayItem?
        get() = items.getOrNull(currentIndex)

    val hasPrevious: Boolean
        get() = currentIndex > 0 && currentIndex < items.size

    val hasNext: Boolean
        get() = currentIndex >= 0 && currentIndex < items.lastIndex

    fun next(): PlaybackQueue? {
        if (!hasNext) return null
        return PlaybackQueue(items, currentIndex + 1)
    }

    fun previous(): PlaybackQueue? {
        if (!hasPrevious) return null
        return PlaybackQueue(items, currentIndex - 1)
    }

    fun moveTo(index: Int): PlaybackQueue? {
        if (index !in items.indices) return null
        return PlaybackQueue(items, index)
    }

    companion object {
        fun create(
            items: List<PlayItem>,
            startIndex: Int = 0,
        ): PlaybackQueue {
            val snapshot = items.toList()
            val safeIndex = if (snapshot.isEmpty()) {
                0
            } else {
                startIndex.coerceIn(snapshot.indices)
            }

            return PlaybackQueue(
                items = snapshot,
                currentIndex = safeIndex,
            )
        }
    }
}
