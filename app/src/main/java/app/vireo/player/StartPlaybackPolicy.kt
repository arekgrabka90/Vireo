package app.vireo.player

/**
 * Tożsamość startu odtwarzania: element oraz generacja ponowienia.
 * `identity` to w praktyce PlayItem; typ ogólny ułatwia testy.
 */
internal data class StartRequest(
    val identity: Any,
    val retryToken: Int,
)

internal enum class StartDecision { START, SKIP_ACTIVE, SKIP_IN_FLIGHT }

/**
 * E3.2: idempotentny start. Ten sam element z tą samą generacją ponowienia
 * nie restartuje źródła, które już gra lub się buforuje, ani startu w toku.
 * Każda inna sytuacja (nowy element, retry, failover, IDLE, ENDED, ERROR)
 * zwraca START i zachowuje dotychczasowe działanie.
 */
internal object StartPlaybackPolicy {
    fun decide(
        last: StartRequest?,
        request: StartRequest,
        resolving: Boolean,
        transport: PlayerTransportState,
    ): StartDecision {
        if (last == null || last != request) return StartDecision.START
        if (resolving) return StartDecision.SKIP_IN_FLIGHT
        return when (transport) {
            PlayerTransportState.READY,
            PlayerTransportState.BUFFERING -> StartDecision.SKIP_ACTIVE
            else -> StartDecision.START
        }
    }
}
