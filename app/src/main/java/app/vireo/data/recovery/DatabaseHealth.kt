package app.vireo.data.recovery

/** Wynik odczytu stanu pliku bazy przed otwarciem przez Room. */
sealed interface DatabaseProbe {
    /** Brak pliku, plik pusty albo baza bez tabel: zostanie utworzona od zera. */
    data object Absent : DatabaseProbe

    data class Readable(val userVersion: Int) : DatabaseProbe

    data class Corrupt(val detail: String) : DatabaseProbe

    data class Unreadable(val detail: String) : DatabaseProbe
}

/** Ocena stanu bazy względem polityki wersji. */
sealed interface DatabaseHealth {
    data object Ready : DatabaseHealth

    /** Baza wspierana, ale starsza niż aplikacja: przed migracją potrzebna kopia. */
    data class NeedsBackup(val fromVersion: Int) : DatabaseHealth

    data class UnsupportedOld(val version: Int) : DatabaseHealth

    data class NewerThanApp(val version: Int) : DatabaseHealth

    data class Corrupt(val detail: String) : DatabaseHealth

    data class Unreadable(val detail: String) : DatabaseHealth
}

/** Klasyfikacja błędu zgłoszonego przy faktycznym otwarciu bazy przez Room. */
sealed interface OpenFailure {
    val detail: String

    data class Corrupt(override val detail: String) : OpenFailure

    data class MissingMigration(override val detail: String) : OpenFailure

    data class NewerThanApp(override val detail: String) : OpenFailure

    data class MigrationFailed(override val detail: String) : OpenFailure

    data class Other(override val detail: String) : OpenFailure
}

object DatabaseClassifier {

    fun classify(
        probe: DatabaseProbe,
        current: Int = DatabasePolicy.CURRENT_VERSION,
        minSupported: Int = DatabasePolicy.MIN_SUPPORTED_VERSION,
    ): DatabaseHealth = when (probe) {
        DatabaseProbe.Absent -> DatabaseHealth.Ready
        is DatabaseProbe.Readable -> classifyVersion(probe.userVersion, current, minSupported)
        is DatabaseProbe.Corrupt -> DatabaseHealth.Corrupt(probe.detail)
        is DatabaseProbe.Unreadable -> DatabaseHealth.Unreadable(probe.detail)
    }

    private fun classifyVersion(version: Int, current: Int, minSupported: Int): DatabaseHealth =
        when {
            version == current -> DatabaseHealth.Ready
            version > current -> DatabaseHealth.NewerThanApp(version)
            version >= minSupported -> DatabaseHealth.NeedsBackup(version)
            else -> DatabaseHealth.UnsupportedOld(version)
        }

    /**
     * Heurystyka zapasowa oparta na typie i treści wyjątku. Podstawą jest odczyt
     * `user_version` przed otwarciem bazy; ta funkcja nazywa to, co mimo to
     * zawiodło przy otwarciu lub migracji.
     */
    fun classifyOpenFailure(error: Throwable): OpenFailure {
        val chain = generateSequence(error) { it.cause }.take(MAX_CHAIN).toList()
        val first = chain.first()
        val rawMessage = first.message.orEmpty().replace(WHITESPACE, " ").trim()
        val detail = (first.javaClass.simpleName + ": " + rawMessage).take(MAX_DETAIL)
        val messages = chain.mapNotNull { it.message?.lowercase() }

        return when {
            chain.any { it.javaClass.name.endsWith("SQLiteDatabaseCorruptException") } ->
                OpenFailure.Corrupt(detail)
            messages.any { "migration from" in it && "required but not found" in it } ->
                OpenFailure.MissingMigration(detail)
            messages.any { "downgrade" in it } ->
                OpenFailure.NewerThanApp(detail)
            messages.any { "migration" in it } ->
                OpenFailure.MigrationFailed(detail)
            else -> OpenFailure.Other(detail)
        }
    }

    private const val MAX_CHAIN = 8
    private const val MAX_DETAIL = 220
    private val WHITESPACE = Regex("\\s+")
}
