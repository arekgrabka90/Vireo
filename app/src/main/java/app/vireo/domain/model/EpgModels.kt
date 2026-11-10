package app.vireo.domain.model

/**
 * Pojedyncza pozycja programu telewizyjnego.
 *
 * Czasy są przechowywane jako Unix epoch milliseconds.
 * Model nie wykonuje żadnych operacji sieciowych.
 */
data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val category: String? = null,
    val iconUrl: String? = null,
) {
    val hasValidTimeRange: Boolean
        get() = startTimeMs >= 0L && endTimeMs > startTimeMs

    fun contains(timeMs: Long): Boolean =
        hasValidTimeRange &&
            timeMs >= startTimeMs &&
            timeMs < endTimeMs
}

/**
 * Lista programów przypisana do identyfikatora kanału EPG.
 */
data class EpgChannelSchedule(
    val channelId: String,
    val programs: List<EpgProgram>,
)

/**
 * Bieżący i następny program dla kanału.
 */
data class EpgNowNext(
    val current: EpgProgram?,
    val next: EpgProgram?,
)
