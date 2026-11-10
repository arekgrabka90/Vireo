package app.vireo.player.engine

import androidx.media3.common.Player
import app.vireo.player.PlayerTransportState
import org.junit.Assert.assertEquals
import org.junit.Test

class Media3PlayerEngineMappingTest {

    @Test
    fun `Media3 states map to engine transport states`() {
        assertEquals(
            PlayerTransportState.IDLE,
            media3TransportState(Player.STATE_IDLE),
        )
        assertEquals(
            PlayerTransportState.BUFFERING,
            media3TransportState(Player.STATE_BUFFERING),
        )
        assertEquals(
            PlayerTransportState.READY,
            media3TransportState(Player.STATE_READY),
        )
        assertEquals(
            PlayerTransportState.ENDED,
            media3TransportState(Player.STATE_ENDED),
        )
    }

    @Test
    fun `error diagnostics redact stream URLs`() {
        val message = sanitizePlayerEngineError(
            "Failed to load https://example.com/live/user/password/123"
        )

        assertEquals(
            "Failed to load [ukryty adres strumienia]",
            message,
        )
    }

    @Test
    fun `blank error message gets a safe fallback`() {
        assertEquals(
            "Playback failed.",
            sanitizePlayerEngineError("  "),
        )
    }
}
