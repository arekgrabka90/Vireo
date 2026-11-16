package app.vireo.data.recovery

/** Trwały magazyn znaczników startu (np. SharedPreferences). */
interface StartMarkerStore {
    var pendingStart: Boolean
    var unstableStarts: Int
}

/**
 * Wykrywa pętlę awarii przy starcie. Start jest "oczekujący" do czasu
 * [markStable] (normalne wyjście albo odpowiednio długa praca). Gdy kolejne
 * starty zaczynają się przy wciąż oczekującym poprzednim, liczone są jako
 * niestabilne; po osiągnięciu progu aplikacja pokazuje ekran odzyskiwania.
 * Przy progu 3 ekran pojawia się przy czwartym starcie.
 */
class StartupCrashGuard(
    private val store: StartMarkerStore,
    private val threshold: Int = DEFAULT_THRESHOLD,
) {

    /** Wywołać raz na start procesu. Zwraca true, gdy próg niestabilnych startów osiągnięto. */
    fun onStart(): Boolean {
        if (store.pendingStart) {
            store.unstableStarts = store.unstableStarts + 1
        } else if (store.unstableStarts != 0) {
            store.unstableStarts = 0
        }
        store.pendingStart = true
        return store.unstableStarts >= threshold
    }

    /** Start uznany za stabilny. */
    fun markStable() {
        if (store.pendingStart) store.pendingStart = false
        if (store.unstableStarts != 0) store.unstableStarts = 0
    }

    /** Użytkownik ponawia po ekranie odzyskiwania; bieżący start pozostaje oczekujący. */
    fun reset() {
        store.unstableStarts = 0
        store.pendingStart = true
    }

    companion object {
        const val DEFAULT_THRESHOLD = 3
    }
}
