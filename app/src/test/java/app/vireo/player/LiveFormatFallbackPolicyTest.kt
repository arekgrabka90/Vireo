package app.vireo.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveFormatFallbackPolicyTest {

    private fun input(
        isLive: Boolean = true,
        xtreamUrl: Boolean = true,
        phase: LiveTunePhase = LiveTunePhase.FIRST_ATTEMPT_BEFORE_READY,
        errorCode: Int = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        httpStatus: Int? = 404,
        format: LiveStreamFormat = LiveStreamFormat.HLS_M3U8,
        fallbackUsed: Boolean = false,
        connectionLimit: Boolean = false,
    ) = LiveFormatFallbackInput(
        isLive = isLive,
        urlBuiltByXtreamProvider = xtreamUrl,
        phase = phase,
        errorCode = errorCode,
        httpStatus = httpStatus,
        currentFormat = format,
        fallbackAlreadyUsedInThisTune = fallbackUsed,
        refusedForConnectionLimit = connectionLimit,
    )

    private fun assertAction(expected: LiveFormatAction, input: LiveFormatFallbackInput) =
        assertEquals(input.toString(), expected, LiveFormatFallbackPolicy.decide(input))

    @Test
    fun `format class errors on first hls attempt switch to ts`() {
        val cases = listOf<Pair<Int, Int?>>(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS to 404,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS to 403,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE to null,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED to null,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED to null,
        )
        cases.forEach { (code, status) ->
            assertAction(
                LiveFormatAction.TRY_MPEG_TS,
                input(errorCode = code, httpStatus = status),
            )
        }
    }

    @Test
    fun `remembered ts with format error gets one hls attempt`() {
        assertAction(
            LiveFormatAction.TRY_HLS_M3U8,
            input(format = LiveStreamFormat.MPEG_TS),
        )
    }

    @Test
    fun `http statuses outside 403 and 404 never fall back`() {
        listOf(null, 200, 401, 410, 429, 500, 502, 503).forEach { status ->
            assertAction(LiveFormatAction.NO_FALLBACK, input(httpStatus = status))
        }
    }

    @Test
    fun `network timeout and unrelated errors never fall back`() {
        listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            Int.MIN_VALUE,
        ).forEach { code ->
            assertAction(
                LiveFormatAction.NO_FALLBACK,
                input(errorCode = code, httpStatus = 404),
            )
        }
    }

    @Test
    fun `only first attempt before ready qualifies`() {
        assertAction(
            LiveFormatAction.NO_FALLBACK,
            input(phase = LiveTunePhase.LATER_ATTEMPT_BEFORE_READY),
        )
        assertAction(
            LiveFormatAction.NO_FALLBACK,
            input(phase = LiveTunePhase.AFTER_READY),
        )
    }

    @Test
    fun `non live or non provider urls never fall back`() {
        assertAction(LiveFormatAction.NO_FALLBACK, input(isLive = false))
        assertAction(LiveFormatAction.NO_FALLBACK, input(xtreamUrl = false))
    }

    @Test
    fun `at most one fallback per tune`() {
        val first = input()
        assertAction(LiveFormatAction.TRY_MPEG_TS, first)
        assertAction(
            LiveFormatAction.NO_FALLBACK,
            input(format = LiveStreamFormat.MPEG_TS, fallbackUsed = true),
        )
    }

    @Test
    fun `connection limit refusal never falls back even with 403`() {
        assertAction(
            LiveFormatAction.NO_FALLBACK,
            input(httpStatus = 403, connectionLimit = true),
        )
    }

    @Test
    fun `format class classification`() {
        assertTrue(
            LiveFormatFallbackPolicy.isFormatClassError(
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 404,
            ),
        )
        assertFalse(
            LiveFormatFallbackPolicy.isFormatClassError(
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, null,
            ),
        )
    }
}
