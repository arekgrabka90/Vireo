package app.vireo.player

import androidx.media3.common.PlaybackException

internal enum class LiveStreamFormat { HLS_M3U8, MPEG_TS }

internal enum class LiveTunePhase {
    FIRST_ATTEMPT_BEFORE_READY,
    LATER_ATTEMPT_BEFORE_READY,
    AFTER_READY,
}

internal enum class LiveFormatAction { NO_FALLBACK, TRY_MPEG_TS, TRY_HLS_M3U8 }

internal data class LiveFormatFallbackInput(
    val isLive: Boolean,
    val urlBuiltByXtreamProvider: Boolean,
    val phase: LiveTunePhase,
    val errorCode: Int,
    val httpStatus: Int?,
    val currentFormat: LiveStreamFormat,
    val fallbackAlreadyUsedInThisTune: Boolean,
    val refusedForConnectionLimit: Boolean = false,
)

/**
 * Pure policy from docs/specs/playback-host.md section 6 (decision 0008).
 * HTTP status extraction from the Media3 cause is intentionally out of scope.
 */
internal object LiveFormatFallbackPolicy {

    fun decide(input: LiveFormatFallbackInput): LiveFormatAction {
        if (!input.isLive || !input.urlBuiltByXtreamProvider) {
            return LiveFormatAction.NO_FALLBACK
        }
        if (input.phase != LiveTunePhase.FIRST_ATTEMPT_BEFORE_READY) {
            return LiveFormatAction.NO_FALLBACK
        }
        if (input.fallbackAlreadyUsedInThisTune) return LiveFormatAction.NO_FALLBACK
        if (input.refusedForConnectionLimit) return LiveFormatAction.NO_FALLBACK
        if (!isFormatClassError(input.errorCode, input.httpStatus)) {
            return LiveFormatAction.NO_FALLBACK
        }
        return when (input.currentFormat) {
            LiveStreamFormat.HLS_M3U8 -> LiveFormatAction.TRY_MPEG_TS
            LiveStreamFormat.MPEG_TS -> LiveFormatAction.TRY_HLS_M3U8
        }
    }

    fun isFormatClassError(errorCode: Int, httpStatus: Int?): Boolean =
        when (errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                httpStatus == 403 || httpStatus == 404

            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> true

            else -> false
        }
}
