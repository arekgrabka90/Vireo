package app.vireo.data.recovery

/**
 * Polityka wersji bazy Room (decyzja P14: minimalna wspierana wersja to 14).
 */
object DatabasePolicy {
    const val DATABASE_NAME = "vireo.db"
    const val MIN_SUPPORTED_VERSION = 14

    /** Musi być równe `@Database(version = ...)` w `VireoDatabase`. */
    const val CURRENT_VERSION = 14

    /** Liczba przechowywanych kopii przedmigracyjnych. */
    const val BACKUPS_TO_KEEP = 2
}
