package app.vireo.player

import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackAlternative(
    val sourceId: Long,
    val streamId: String,
    val url: String,
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

data class PlaybackEpisode(
    val title: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val description: String? = null,
    val duration: String? = null,
    val thumbnailUrl: String? = null,
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
    val sourceId: Long? = null,
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val description: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val episodeDescription: String? = null,
    val episodeThumbnailUrl: String? = null,
    val episodes: List<PlaybackEpisode> = emptyList(),
)

@Singleton
class PlaybackSession @Inject constructor() {
    var items: List<PlayItem> = emptyList()
        private set
    var index: Int = 0
        private set

    private var activeAlternativeIndex: Int = 0

    @Synchronized
    fun start(items: List<PlayItem>, index: Int) {
        this.items = items.toList()
        this.index = if (this.items.isEmpty()) {
            0
        } else {
            index.coerceIn(this.items.indices)
        }
        activeAlternativeIndex = 0
    }

    @Synchronized
    fun current(): PlayItem? {
        val item = items.getOrNull(index) ?: return null
        if (activeAlternativeIndex == 0) return item

        val alternative = item.alternatives.getOrNull(
            activeAlternativeIndex - 1
        ) ?: return item

        return item.copy(
            url = alternative.url,
            sourceId = alternative.sourceId,
            streamId = alternative.streamId,
            seriesId = alternative.seriesId ?: item.seriesId,
            seasonNumber = alternative.seasonNumber ?: item.seasonNumber,
            episodeNumber = alternative.episodeNumber ?: item.episodeNumber,
        )
    }

    @Synchronized
    fun nextAlternative(): PlayItem? {
        val item = items.getOrNull(index) ?: return null
        if (activeAlternativeIndex >= item.alternatives.size) return null

        activeAlternativeIndex++
        return current()
    }

    @Synchronized
    fun hasPrevious(): Boolean =
        items.isNotEmpty() && index > 0 && index < items.size

    @Synchronized
    fun hasNext(): Boolean =
        items.isNotEmpty() && index >= 0 && index < items.lastIndex

    @Synchronized
    fun previous(): PlayItem? {
        if (!hasPrevious()) return null
        index -= 1
        activeAlternativeIndex = 0
        return current()
    }

    @Synchronized
    fun next(): PlayItem? {
        if (!hasNext()) return null
        index += 1
        activeAlternativeIndex = 0
        return current()
    }
}
