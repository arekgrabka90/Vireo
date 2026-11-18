package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Test

class StartPlaybackPolicyTest {

    private val a0 = StartRequest("item-a", 0)

    private fun decide(
        last: StartRequest?,
        request: StartRequest = a0,
        resolving: Boolean = false,
        transport: PlayerTransportState = PlayerTransportState.IDLE,
    ) = StartPlaybackPolicy.decide(last, request, resolving, transport)

    @Test
    fun `first start always starts`() {
        for (t in PlayerTransportState.entries) {
            assertEquals("transport=$t", StartDecision.START, decide(null, transport = t))
            assertEquals(
                "transport=$t resolving",
                StartDecision.START,
                decide(null, resolving = true, transport = t),
            )
        }
    }

    @Test
    fun `different item starts even when the engine is active`() {
        assertEquals(
            StartDecision.START,
            decide(StartRequest("item-b", 0), transport = PlayerTransportState.READY),
        )
    }

    @Test
    fun `new retry generation starts even when the engine is active`() {
        assertEquals(
            StartDecision.START,
            decide(StartRequest("item-a", 1), transport = PlayerTransportState.READY),
        )
        assertEquals(
            StartDecision.START,
            decide(StartRequest("item-a", 1), transport = PlayerTransportState.BUFFERING),
        )
    }

    @Test
    fun `superseding request starts while another is resolving`() {
        assertEquals(
            StartDecision.START,
            decide(StartRequest("item-b", 0), resolving = true),
        )
        assertEquals(
            StartDecision.START,
            decide(StartRequest("item-a", 1), resolving = true),
        )
    }

    @Test
    fun `same request in flight is skipped for every transport state`() {
        for (t in PlayerTransportState.entries) {
            assertEquals(
                "transport=$t",
                StartDecision.SKIP_IN_FLIGHT,
                decide(a0, resolving = true, transport = t),
            )
        }
    }

    @Test
    fun `same request is skipped only when ready or buffering`() {
        for (t in PlayerTransportState.entries) {
            val expected =
                if (t == PlayerTransportState.READY || t == PlayerTransportState.BUFFERING) {
                    StartDecision.SKIP_ACTIVE
                } else {
                    StartDecision.START
                }
            assertEquals("transport=$t", expected, decide(a0, transport = t))
        }
    }

    @Test
    fun `same request restarts after idle ended or error`() {
        assertEquals(StartDecision.START, decide(a0, transport = PlayerTransportState.IDLE))
        assertEquals(StartDecision.START, decide(a0, transport = PlayerTransportState.ENDED))
        assertEquals(StartDecision.START, decide(a0, transport = PlayerTransportState.ERROR))
    }

    @Test
    fun `requests with equal identity and token are equal`() {
        assertEquals(StartRequest("x", 3), StartRequest("x", 3))
    }
}
