package app.vireo.player

import javax.inject.Inject
import javax.inject.Singleton

data class PlayItem(
    val title: String,
    val url: String,
    val contentKey: String? = null,
    val contentType: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
)

@Singleton
class PlaybackSession @Inject constructor() {
    var items: List<PlayItem> = emptyList()
        private set
    var index: Int = 0
        private set

    fun start(items: List<PlayItem>, index: Int) {
        this.items = items
        this.index = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    fun current(): PlayItem? = items.getOrNull(index)

    fun hasNext(): Boolean = index < items.lastIndex

    fun next(): PlayItem? {
        if (!hasNext()) return null
        index += 1
        return current()
    }
}
