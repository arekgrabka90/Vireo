package app.vireo.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRecoveryPolicyTest {

    @Test
    fun `network and timeout failures retry current source`() {
        val codes = listOf(
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
            PlaybackException.ERROR_CODE_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
        )

        codes.forEach { code ->
            assertEquals(
                "Unexpected action for error code $code",
                PlaybackRecoveryAction.RETRY_CURRENT,
                PlaybackRecoveryPolicy.classify(code),
            )
        }
    }

    @Test
    fun `source and format failures advance to alternative`() {
        val codes = listOf(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED,
        )

        codes.forEach { code ->
            assertEquals(
                "Unexpected action for error code $code",
                PlaybackRecoveryAction.FAILOVER,
                PlaybackRecoveryPolicy.classify(code),
            )
        }
    }

    @Test
    fun `unknown errors are fatal and are not retried automatically`() {
        assertEquals(
            PlaybackRecoveryAction.FATAL,
            PlaybackRecoveryPolicy.classify(Int.MIN_VALUE),
        )
    }

    @Test
    fun `current source retry budget is limited to two attempts`() {
        assertEquals(2, PlaybackRecoveryPolicy.MAX_CURRENT_SOURCE_RETRIES)
    }
}
