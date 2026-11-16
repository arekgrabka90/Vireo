package app.vireo.data.recovery

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Kopia przedmigracyjna zapisana w osobnym katalogu. */
data class BackupInfo(
    val dir: File,
    val version: Int,
    val timestamp: String,
    val sequence: Int,
    val sizeBytes: Long,
)

/**
 * Operacje na plikach bazy: kopie bezpieczeństwa, odsunięcie i przywracanie.
 *
 * Nic tu nie usuwa danych użytkownika bez zachowania ich kopii: odsunięcie
 * przenosi pliki do katalogu `aside-*`, który nie jest czyszczony automatycznie.
 * Przycinane są wyłącznie kopie przedmigracyjne (patrz
 * [DatabasePolicy.BACKUPS_TO_KEEP]). Operacje należy wykonywać, gdy baza nie
 * jest otwarta przez Room.
 */
class DatabaseFiles(
    private val dbFile: File,
    private val backupDir: File,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val usableSpace: (File) -> Long = { it.usableSpace },
) {
    private val dbDir: File = dbFile.absoluteFile.parentFile
        ?: throw IllegalArgumentException("Plik bazy musi mieć katalog nadrzędny")

    private val names: List<String> =
        listOf(dbFile.name) + SIDECAR_SUFFIXES.map { dbFile.name + it }

    private fun currentFiles(): List<File> =
        names.map { File(dbDir, it) }.filter { it.isFile }

    fun hasDatabase(): Boolean = dbFile.isFile && dbFile.length() > 0L

    fun currentSizeBytes(): Long = currentFiles().sumOf { it.length() }

    /** Czy jest miejsce na kopię (rozmiar plików bazy + 20% + 1 MB zapasu). */
    fun hasSpaceForBackup(): Boolean {
        backupDir.mkdirs()
        val needed = currentSizeBytes() * 12 / 10 + 1_048_576L
        return usableSpace(backupDir) >= needed
    }

    /** Kopiuje plik bazy i towarzyszące pliki do nowego katalogu kopii. */
    fun createBackup(fromVersion: Int, pruneAfter: Boolean = true): File {
        val files = currentFiles()
        if (files.none { it.name == dbFile.name }) {
            throw IOException("Brak pliku bazy do skopiowania")
        }
        val target = uniqueDir("vireo-v$fromVersion-${timestamp()}")
        try {
            for (file in files) {
                val copy = File(target, file.name)
                file.copyTo(copy, overwrite = false)
                if (copy.length() != file.length()) {
                    throw IOException("Niezgodny rozmiar kopii: ${file.name}")
                }
            }
        } catch (e: Exception) {
            target.deleteRecursively()
            throw e
        }
        if (pruneAfter) pruneBackups()
        return target
    }

    /** Kopie przedmigracyjne, od najnowszej. */
    fun listBackups(): List<BackupInfo> =
        (backupDir.listFiles() ?: emptyArray<File>())
            .filter { it.isDirectory }
            .mapNotNull { dir ->
                val match = BACKUP_NAME.matchEntire(dir.name) ?: return@mapNotNull null
                BackupInfo(
                    dir = dir,
                    version = match.groupValues[1].toInt(),
                    timestamp = match.groupValues[2],
                    sequence = match.groupValues[3].toIntOrNull() ?: 0,
                    sizeBytes = dir.listFiles()?.sumOf { it.length() } ?: 0L,
                )
            }
            .sortedWith(
                compareByDescending<BackupInfo> { it.timestamp }
                    .thenByDescending { it.sequence }
            )

    /** Przenosi bieżące pliki bazy do katalogu `aside-*` (bez usuwania danych). */
    fun moveAside(reason: String): File {
        val safeReason = reason.filter { it.isLetterOrDigit() || it == '-' }.ifEmpty { "aside" }
        val target = uniqueDir("aside-$safeReason-${timestamp()}")
        val moved = mutableListOf<Pair<File, File>>()
        try {
            for (file in currentFiles()) {
                val destination = File(target, file.name)
                moveFile(file, destination)
                moved += file to destination
            }
        } catch (e: Exception) {
            moved.asReversed().forEach { (original, destination) ->
                runCatching { moveFile(destination, original) }
            }
            runCatching { target.delete() }
            throw e
        }
        return target
    }

    /**
     * Przywraca kopię. Bieżące pliki są najpierw odsunięte; zwraca katalog z
     * odsuniętymi plikami albo null, gdy nie było czego odsuwać.
     */
    fun restoreBackup(backup: BackupInfo): File? {
        val source = File(backup.dir, dbFile.name)
        if (!source.isFile) throw IOException("Kopia nie zawiera pliku bazy")
        val aside = if (currentFiles().isEmpty()) null else moveAside("before-restore")
        try {
            for (name in names) {
                val file = File(backup.dir, name)
                if (!file.isFile) continue
                val destination = File(dbDir, name)
                file.copyTo(destination, overwrite = false)
                if (destination.length() != file.length()) {
                    throw IOException("Niezgodny rozmiar po przywróceniu: $name")
                }
            }
        } catch (e: Exception) {
            currentFiles().forEach { runCatching { it.delete() } }
            aside?.listFiles()?.forEach { runCatching { moveFile(it, File(dbDir, it.name)) } }
            throw e
        }
        return aside
    }

    /** Usuwa kopie przedmigracyjne ponad limit [DatabasePolicy.BACKUPS_TO_KEEP] (najstarsze). */
    fun pruneBackups() {
        listBackups().drop(DatabasePolicy.BACKUPS_TO_KEEP).forEach { it.dir.deleteRecursively() }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(clock()))

    private fun uniqueDir(base: String): File {
        backupDir.mkdirs()
        var candidate = File(backupDir, base)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(backupDir, "$base-$counter")
            counter++
        }
        if (!candidate.mkdirs()) throw IOException("Nie można utworzyć katalogu: ${candidate.name}")
        return candidate
    }

    private fun moveFile(from: File, to: File) {
        if (from.renameTo(to)) return
        from.copyTo(to, overwrite = false)
        if (to.length() != from.length()) {
            to.delete()
            throw IOException("Niezgodny rozmiar po przeniesieniu: ${from.name}")
        }
        if (!from.delete()) {
            throw IOException("Nie można usunąć źródła po kopiowaniu: ${from.name}")
        }
    }

    private companion object {
        val SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")
        val BACKUP_NAME = Regex("^vireo-v(\\d+)-(\\d{8}-\\d{6})(?:-(\\d+))?$")
    }
}
