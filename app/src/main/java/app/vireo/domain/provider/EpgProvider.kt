package app.vireo.domain.provider

import app.vireo.domain.model.EpgChannelSchedule

/**
 * Kontrakt dostępu do danych EPG.
 *
 * Jest to wyłącznie interfejs domenowy.
 * W Foundation Pack 1 nie powstaje implementacja pobierająca XMLTV
 * ani żaden mechanizm sieciowy.
 */
interface EpgProvider {

    /**
     * Zwraca harmonogram dla wskazanego stream_id Xtream.
     *
     * Zakres czasu jest wyrażony w Unix epoch milliseconds.
     */
    suspend fun schedule(
        streamId: String,
        fromTimeMs: Long,
        toTimeMs: Long,
    ): Result<EpgChannelSchedule>
}
