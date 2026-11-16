package app.vireo.ui.recovery

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.local.VireoDatabase
import app.vireo.data.recovery.DatabaseClassifier
import app.vireo.data.recovery.DatabaseFiles
import app.vireo.data.recovery.DatabaseHealth
import app.vireo.data.recovery.DatabasePolicy
import app.vireo.data.recovery.OpenFailure
import app.vireo.data.recovery.SharedPrefsStartMarkerStore
import app.vireo.data.recovery.SqliteDatabaseInspector
import app.vireo.data.recovery.StartupCrashGuard
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Kontrola stanu bazy przy starcie aplikacji. Nic nie dotyka Room, dopóki
 * odczyt wersji pliku (bez usuwania czegokolwiek), licznik niestabilnych
 * startów i ewentualna zweryfikowana kopia przedmigracyjna nie zakończą się
 * powodzeniem.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: Lazy<VireoDatabase>,
) : ViewModel() {

    private val dbFile: File = context.getDatabasePath(DatabasePolicy.DATABASE_NAME)
    private val dbDir: File = requireNotNull(dbFile.absoluteFile.parentFile)
    private val files = DatabaseFiles(dbFile, File(context.filesDir, "db_backups"))
    private val crashGuard = StartupCrashGuard(SharedPrefsStartMarkerStore(context))

    private val _state = MutableStateFlow<StartupState>(StartupState.Checking)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var skipBackup = false
    private var guardEvaluated = false
    private var crashLoopSuspected = false
    private var checkJob: Job? = null

    init {
        recheck()
    }

    fun recheck() {
        if (checkJob?.isActive == true) return
        checkJob = viewModelScope.launch {
            _state.value = StartupState.Checking
            _state.value = withContext(Dispatchers.IO) { runCheck() }
        }
    }

    /** "Spróbuj ponownie": zeruje licznik niestabilnych startów i ponawia kontrolę. */
    fun retry() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { crashGuard.reset() }
            crashLoopSuspected = false
            recheck()
        }
    }

    /** Wywoływane, gdy start uznano za stabilny (normalne wyjście albo odpowiednio długa praca). */
    fun markStable() {
        viewModelScope.launch(Dispatchers.IO) { crashGuard.markStable() }
    }

    fun continueWithoutBackup() {
        skipBackup = true
        recheck()
    }

    fun restoreLatestBackup() {
        runFileAction(
            working = "Przywracanie kopii…",
            done = "Kopia została przywrócona. Poprzednie pliki bazy odsunięto (nie usunięto).",
        ) {
            val candidate = files.listBackups().firstOrNull {
                SqliteDatabaseInspector(File(it.dir, dbFile.name)).integrityIssue() == null
            } ?: throw IOException("Żadna kopia nie przeszła kontroli spójności")
            files.restoreBackup(candidate)
        }
    }

    fun moveDatabaseAside() {
        runFileAction(
            working = "Odsuwanie bazy…",
            done = "Baza została odsunięta (nie usunięta). Po ponownym uruchomieniu aplikacja utworzy nową, pustą bazę.",
        ) {
            files.moveAside("reset")
        }
    }

    fun suggestedExportName(): String = "vireo-db-${timestamp()}.zip"

    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            _notice.value = "Eksportowanie…"
            _notice.value = withContext(Dispatchers.IO) {
                try {
                    val out = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("Nie można otworzyć pliku docelowego")
                    out.use { writeZip(it) }
                    "Wyeksportowano kopię bazy."
                } catch (e: Exception) {
                    "Eksport nie powiódł się: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    /** Zapasowy eksport, gdy urządzenie nie ma systemowego wyboru plików (np. TV). */
    fun exportToAppFolder() {
        viewModelScope.launch {
            _notice.value = "Eksportowanie…"
            _notice.value = withContext(Dispatchers.IO) {
                try {
                    val base = context.getExternalFilesDir(null)
                        ?: throw IOException("Brak dostępnego katalogu eksportu")
                    val dir = File(base, "exports")
                    if (!dir.isDirectory && !dir.mkdirs()) {
                        throw IOException("Nie można utworzyć katalogu eksportu")
                    }
                    val target = File(dir, suggestedExportName())
                    target.outputStream().use { writeZip(it) }
                    "Zapisano: ${target.absolutePath}"
                } catch (e: Exception) {
                    "Eksport nie powiódł się: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    private fun runCheck(): StartupState {
        if (!guardEvaluated) {
            guardEvaluated = true
            crashLoopSuspected = crashGuard.onStart()
        }
        if (crashLoopSuspected) {
            return recovery(
                RecoveryKind.REPEATED_CRASHES,
                "kilka kolejnych niestabilnych uruchomień",
            )
        }

        when (val health = DatabaseClassifier.classify(SqliteDatabaseInspector(dbFile).probe())) {
            DatabaseHealth.Ready -> Unit
            is DatabaseHealth.NeedsBackup -> {
                if (!skipBackup) {
                    val problem = tryCreateBackup(health.fromVersion)
                    if (problem != null) return recovery(RecoveryKind.BACKUP_FAILED, problem)
                }
            }
            is DatabaseHealth.UnsupportedOld ->
                return recovery(RecoveryKind.UNSUPPORTED_OLD, "wersja bazy: ${health.version}")
            is DatabaseHealth.NewerThanApp ->
                return recovery(RecoveryKind.NEWER_THAN_APP, "wersja bazy: ${health.version}")
            is DatabaseHealth.Corrupt -> return recovery(RecoveryKind.CORRUPT, health.detail)
            is DatabaseHealth.Unreadable -> return recovery(RecoveryKind.UNREADABLE, health.detail)
        }
        return openRoom()
    }

    private fun tryCreateBackup(fromVersion: Int): String? {
        if (!files.hasSpaceForBackup()) return "za mało wolnego miejsca na kopię"
        return try {
            val backup = files.createBackup(fromVersion, pruneAfter = false)
            val issue = SqliteDatabaseInspector(File(backup, dbFile.name)).integrityIssue()
            if (issue != null) {
                backup.deleteRecursively()
                "kopia nie przeszła kontroli spójności: $issue"
            } else {
                files.pruneBackups()
                null
            }
        } catch (e: Exception) {
            e.message ?: "błąd zapisu kopii"
        }
    }

    private fun openRoom(): StartupState = try {
        database.get().openHelper.writableDatabase
        StartupState.Ready
    } catch (e: Exception) {
        val failure = DatabaseClassifier.classifyOpenFailure(e)
        recovery(kindOf(failure), failure.detail)
    }

    private fun kindOf(failure: OpenFailure): RecoveryKind = when (failure) {
        is OpenFailure.Corrupt -> RecoveryKind.CORRUPT
        is OpenFailure.MissingMigration -> RecoveryKind.MISSING_MIGRATION
        is OpenFailure.NewerThanApp -> RecoveryKind.NEWER_THAN_APP
        is OpenFailure.MigrationFailed -> RecoveryKind.MIGRATION_FAILED
        is OpenFailure.Other -> RecoveryKind.OPEN_FAILED
    }

    private fun recovery(kind: RecoveryKind, detail: String): StartupState.Recovery =
        StartupState.Recovery(
            kind = kind,
            detail = detail,
            hasDatabase = files.hasDatabase(),
            latestBackup = files.listBackups().firstOrNull(),
        )

    private fun runFileAction(working: String, done: String, action: () -> Any?) {
        viewModelScope.launch {
            _state.value = StartupState.Working(working)
            val error = withContext(Dispatchers.IO) {
                try {
                    runCatching { database.get().close() }
                    action()
                    crashGuard.markStable()
                    null
                } catch (e: Exception) {
                    e.message ?: e.javaClass.simpleName
                }
            }
            _state.value = if (error == null) {
                StartupState.RestartRequired(done)
            } else {
                recovery(RecoveryKind.OPEN_FAILED, "Operacja nie powiodła się: $error")
            }
        }
    }

    private fun writeZip(out: OutputStream) {
        val names = listOf(dbFile.name) + listOf("-wal", "-shm", "-journal").map { dbFile.name + it }
        val existing = names.map { File(dbDir, it) }.filter { it.isFile }
        if (existing.isEmpty()) throw IOException("Brak plików bazy do eksportu")
        ZipOutputStream(out).use { zip ->
            for (file in existing) {
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
}
