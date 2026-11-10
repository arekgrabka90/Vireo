package app.vireo.player

/**
 * Stan transportu odtwarzacza.
 *
 * Model jest niezależny od Compose i Media3.
 */
enum class PlayerTransportState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR,
}

/**
 * Informacje o pozycji odtwarzania.
 *
 * Ten model powinien być obserwowany niezależnie od stanu HUD,
 * aby częste aktualizacje czasu nie przebudowywały całej nakładki.
 */
data class PlayerProgressState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isSeekable: Boolean = false,
) {
    val normalizedPositionMs: Long
        get() = positionMs.coerceAtLeast(0L)

    val normalizedDurationMs: Long
        get() = durationMs.coerceAtLeast(0L)

    val normalizedBufferedPositionMs: Long
        get() = bufferedPositionMs
            .coerceAtLeast(0L)
            .let { buffered ->
                if (normalizedDurationMs > 0L) {
                    buffered.coerceAtMost(normalizedDurationMs)
                } else {
                    buffered
                }
            }
}

/**
 * Kontekst interfejsu playera.
 *
 * Nie przechowuje obiektu ExoPlayer i nie zarządza jego cyklem życia.
 */
data class PlayerRuntimeState(
    val title: String = "",
    val contentType: String? = null,
    val transportState: PlayerTransportState = PlayerTransportState.IDLE,
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val controlsVisible: Boolean = true,
    val selectedAudioTrack: String? = null,
    val selectedSubtitleTrack: String? = null,
    val errorMessage: String? = null,
) {
    val hasError: Boolean
        get() = !errorMessage.isNullOrBlank()
}

/**
 * Bezpieczna normalizacja danych postępu.
 */
internal fun normalizePlayerProgress(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    isSeekable: Boolean,
): PlayerProgressState {
    val duration = durationMs.coerceAtLeast(0L)
    val position = positionMs.coerceAtLeast(0L).let {
        if (duration > 0L) it.coerceAtMost(duration) else it
    }
    val buffered = bufferedPositionMs.coerceAtLeast(0L).let {
        if (duration > 0L) it.coerceAtMost(duration) else it
    }

    return PlayerProgressState(
        positionMs = position,
        durationMs = duration,
        bufferedPositionMs = buffered,
        isSeekable = isSeekable && duration > 0L,
    )
}
