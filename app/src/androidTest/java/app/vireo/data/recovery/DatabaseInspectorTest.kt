package app.vireo.data.recovery

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vireo.data.local.VireoDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseInspectorTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        dir = File(cache, "inspector-" + System.nanoTime()).apply { mkdirs() }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun createDatabase(name: String, version: Int?): File {
        val file = File(dir, name)
        val db = SQLiteDatabase.openOrCreateDatabase(file.path, null)
        try {
            db.execSQL("CREATE TABLE sample (id INTEGER PRIMARY KEY)")
            if (version != null) db.version = version
        } finally {
            db.close()
        }
        return file
    }

    @Test
    fun missingFileIsAbsent() {
        assertEquals(DatabaseProbe.Absent, SqliteDatabaseInspector(File(dir, "none.db")).probe())
    }

    @Test
    fun emptyFileIsAbsent() {
        val file = File(dir, "empty.db").apply { writeBytes(ByteArray(0)) }
        assertEquals(DatabaseProbe.Absent, SqliteDatabaseInspector(file).probe())
    }

    @Test
    fun databaseWithVersionIsReadable() {
        val file = createDatabase("v14.db", 14)
        assertEquals(DatabaseProbe.Readable(14), SqliteDatabaseInspector(file).probe())
    }

    @Test
    fun tablesWithVersionZeroAreReadableAsZero() {
        val file = createDatabase("v0.db", null)
        assertEquals(DatabaseProbe.Readable(0), SqliteDatabaseInspector(file).probe())
    }

    @Test
    fun garbageFileIsNotReadableAndIsNotDeleted() {
        val file = File(dir, "garbage.db")
        file.writeBytes(ByteArray(4096) { (it % 251).toByte() })

        val probe = SqliteDatabaseInspector(file).probe()

        assertTrue(probe is DatabaseProbe.Corrupt || probe is DatabaseProbe.Unreadable)
        assertTrue(file.exists())
        assertEquals(4096L, file.length())
    }

    @Test
    fun currentVersionMatchesRoomDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, VireoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(DatabasePolicy.CURRENT_VERSION, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }

    @Test
    fun healthyDatabasePassesIntegrityCheck() {
        val file = createDatabase("ok.db", 14)
        assertTrue(SqliteDatabaseInspector(file).integrityIssue() == null)
    }

    @Test
    fun garbageFileFailsIntegrityCheckAndIsNotDeleted() {
        val file = File(dir, "garbage-check.db")
        file.writeBytes(ByteArray(4096) { (it % 251).toByte() })

        assertTrue(SqliteDatabaseInspector(file).integrityIssue() != null)
        assertTrue(file.exists())
        assertEquals(4096L, file.length())
    }

    @Test
    fun damagedBtreePageFailsIntegrityCheck() {
        val file = File(dir, "pages.db")
        val db = SQLiteDatabase.openOrCreateDatabase(file.path, null)
        try {
            db.execSQL("CREATE TABLE sample (id INTEGER PRIMARY KEY, payload TEXT)")
            db.beginTransaction()
            try {
                for (i in 1..2000) {
                    db.execSQL(
                        "INSERT INTO sample (payload) VALUES (?)",
                        arrayOf<Any?>("x".repeat(100) + i),
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            db.version = 14
        } finally {
            db.close()
        }
        java.io.RandomAccessFile(file, "rw").use { raf ->
            for (page in 1..3) {
                raf.seek(page * 4096L)
                raf.write(0xFF)
            }
        }

        assertTrue(SqliteDatabaseInspector(file).integrityIssue() != null)
    }
}
