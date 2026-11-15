package app.vireo.data.security

/** Wynik weryfikacji PIN-u z uwzględnieniem limitu prób. */
sealed interface PinCheckResult {
    data object Success : PinCheckResult

    /** Błędny PIN; [attemptsLeftBeforeLock] to liczba darmowych prób do blokady. */
    data class Wrong(val attemptsLeftBeforeLock: Int) : PinCheckResult

    /** Weryfikacja zablokowana na [remainingMs] milisekund. */
    data class Locked(val remainingMs: Long) : PinCheckResult
}
