package app.vireo.data.security

/**
 * Hasło konta nie daje się odszyfrować (np. po utracie klucza w Android Keystore).
 * Konto, katalog i biblioteka pozostają nietknięte; hasło trzeba wpisać ponownie.
 */
class CredentialUnavailableException(
    val accountId: Long,
    cause: Throwable? = null,
) : IllegalStateException(
    "Hasło do źródła wymaga ponownego wpisania (Ustawienia → Źródła). Nie można go odczytać.",
    cause,
)
