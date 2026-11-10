package app.vireo.player

import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackAlternative(
    val sourceId: Long,
    val streamId: String,
    val url: String,
)

data class PlayItem(
    val title: String,
    val url: String,
    val contentKey: String? = null,
    val contentType: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val alternatives: List<PlaybackAlternative> = emptyList(),
    val epgChannelId: String? = null,
    val streamId: String? = null,
)

@Singleton
class PlaybackSession @Inject constructor() {
    var items: List<PlayItem> = emptyList()
        private set
    var index: Int = 0
        private set

    private var activeAlternativeIndex: Int = 0

    fun start(items: List<PlayItem>, index: Int) {
        this.items = items
        this.index = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        activeAlternativeIndex = 0
    }

    fun current(): PlayItem? {
        val item = items.getOrNull(index) ?: return null
        if (activeAlternativeIndex == 0) return item

        val alternative = item.alternatives.getOrNull(activeAlternativeIndex - 1)
            ?: return item

        return item.copy(url = alternative.url)
    }

    fun nextAlternative(): PlayItem? {
        val item = items.getOrNull(index) ?: return null
        if (activeAlternativeIndex >= item.alternatives.size) return null

        activeAlternativeIndex++
        return current()
    }

    fun hasNext(): Boolean = index < items.lastIndex

    fun next(): PlayItem? {
        if (!hasNext()) return null
        index += 1
        activeAlternativeIndex = 0
        return current()
    }
}
