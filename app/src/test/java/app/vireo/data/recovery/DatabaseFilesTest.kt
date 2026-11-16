package app.vireo.data.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class DatabaseFilesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var dbFile: File
    private lateinit var walFile: File
    private lateinit var backupDir: File
    private var now = 1_700_000_000_000L
    private var space = 100_000_000L

    @Before
    fun setUp() {
        val dbDir = tmp.newFolder("databases")
        dbFile = File(dbDir, "vireo.db")
        walFile = File(dbDir, "vireo.db-wal")
        backupDir = File(tmp.root, "db_backups")
    }

    private fun files() = DatabaseFiles(dbFile, backupDir, clock = { now }, usableSpace = { space })

    @Test
    fun createBackupCopiesDatabaseAndWal() {
        dbFile.writeText("main")
        walFile.writeText("wal")

        val backup = files().createBackup(13)

        assertTrue(Regex("vireo-v13-\\d{8}-\\d{6}").matches(backup.name))
        assertEquals("main", File(backup, "vireo.db").readText())
        assertEquals("wal", File(backup, "vireo.db-wal").readText())
        assertTrue(dbFile.isFile)
        assertTrue(walFile.isFile)
    }

    @Test
    fun createBackupWithoutDatabaseThrows() {
        assertThrows(IOException::class.java) { files().createBackup(13) }
    }

    @Test
    fun createBackupInSameSecondGetsUniqueDirectory() {
        dbFile.writeText("main")
        val manager = files()

        val first = manager.createBackup(13)
        val second = manager.createBackup(13)

        assertNotEquals(first.name, second.name)
        assertEquals(2, manager.listBackups().size)
    }

    @Test
    fun pruneKeepsOnlyTwoNewestBackups() {
        dbFile.writeText("main")
        val manager = files()

        for (version in 11..14) {
            manager.createBackup(version)
            now += 1_000L
        }

        assertEquals(listOf(14, 13), manager.listBackups().map { it.version })
    }

    @Test
    fun listBackupsIgnoresUnrelatedEntries() {
        dbFile.writeText("main")
        val manager = files()
        manager.createBackup(13)
        File(backupDir, "other").mkdirs()
        File(backupDir, "vireo-v13-x").mkdirs()
        File(backupDir, "stray.txt").writeText("x")

        assertEquals(1, manager.listBackups().size)
    }

    @Test
    fun moveAsideMovesAllDatabaseFiles() {
        dbFile.writeText("main")
        walFile.writeText("wal")

        val aside = files().moveAside("corrupt")

        assertTrue(aside.name.startsWith("aside-corrupt-"))
        assertFalse(dbFile.exists())
        assertFalse(walFile.exists())
        assertEquals("main", File(aside, "vireo.db").readText())
        assertEquals("wal", File(aside, "vireo.db-wal").readText())
    }

    @Test
    fun restoreBackupReplacesCurrentFilesAndKeepsThemAside() {
        dbFile.writeText("old")
        val manager = files()
        val backup = manager.createBackup(13)
        now += 5_000L
        dbFile.writeText("new")
        walFile.writeText("newwal")

        val aside = manager.restoreBackup(manager.listBackups().first { it.dir == backup })

        assertNotNull(aside)
        assertEquals("old", dbFile.readText())
        assertFalse(walFile.exists())
        assertEquals("new", File(aside!!, "vireo.db").readText())
        assertEquals("newwal", File(aside, "vireo.db-wal").readText())
    }

    @Test
    fun restoreBackupWithoutDatabaseFileKeepsCurrentFiles() {
        dbFile.writeText("current")
        val emptyBackupDir = File(backupDir, "vireo-v13-20260101-000000").apply { mkdirs() }
        val info = BackupInfo(emptyBackupDir, 13, "20260101-000000", 0, 0L)

        assertThrows(IOException::class.java) { files().restoreBackup(info) }

        assertEquals("current", dbFile.readText())
        assertNull(File(backupDir, "aside-before-restore").takeIf { it.exists() })
    }

    @Test
    fun hasSpaceForBackupUsesInjectedFreeSpace() {
        dbFile.writeBytes(ByteArray(1_000))
        val manager = files()

        space = 1_000L
        assertFalse(manager.hasSpaceForBackup())

        space = 10_000_000L
        assertTrue(manager.hasSpaceForBackup())
    }

    @Test
    fun createBackupWithoutPruningKeepsAllBackups() {
        dbFile.writeText("main")
        val manager = files()

        for (version in 11..14) {
            manager.createBackup(version, pruneAfter = false)
            now += 1_000L
        }

        assertEquals(4, manager.listBackups().size)
    }

    @Test
    fun pruneBackupsRemovesOldestBeyondLimit() {
        dbFile.writeText("main")
        val manager = files()
        for (version in 11..14) {
            manager.createBackup(version, pruneAfter = false)
            now += 1_000L
        }

        manager.pruneBackups()

        assertEquals(listOf(14, 13), manager.listBackups().map { it.version })
    }
}
