package app.vireo.data.security

/**
 * Czysta polityka limitu prób PIN-u profilu.
 *
 * Pierwsze [FREE_ATTEMPTS] błędnych prób nie powoduje blokady. Każda kolejna
 * błędna próba blokuje weryfikację na [BASE_LOCK_MS], a czas blokady podwaja
 * się z każdą następną porażką, do limitu [MAX_LOCK_MS].
 *
 * Polityka nie jest zabezpieczeniem przed manipulacją zegarem urządzenia ani
 * przed czyszczeniem danych aplikacji; ogranicza zgadywanie PIN-u z poziomu UI.
 */
object PinAttemptPolicy {
    const val FREE_ATTEMPTS = 5
    const val BASE_LOCK_MS = 30_000L
    const val MAX_LOCK_MS = 15 * 60_000L

    private const val MAX_EXPONENT = 10

    fun lockDurationMs(failures: Int): Long {
        if (failures < FREE_ATTEMPTS) return 0L
        val exponent = (failures - FREE_ATTEMPTS).coerceAtMost(MAX_EXPONENT)
        return (BASE_LOCK_MS shl exponent).coerceAtMost(MAX_LOCK_MS)
    }

    /**
     * Pozostały czas blokady. Gdy zegar został cofnięty ([nowMs] mniejsze niż
     * [lastFailureAtMs]), zwracany jest pełny czas blokady; wywołujący powinien
     * wtedy przesunąć znacznik ostatniej porażki na [nowMs].
     */
    fun remainingLockMs(failures: Int, lastFailureAtMs: Long, nowMs: Long): Long {
        val duration = lockDurationMs(failures)
        if (duration == 0L) return 0L
        if (nowMs < lastFailureAtMs) return duration
        return (lastFailureAtMs + duration - nowMs).coerceAtLeast(0L)
    }

    fun attemptsLeftBeforeLock(failures: Int): Int =
        (FREE_ATTEMPTS - failures).coerceAtLeast(0)
}
