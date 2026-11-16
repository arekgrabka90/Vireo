package app.vireo.data.recovery

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vireo.data.local.ProfileEntity
import app.vireo.data.local.VireoDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile

@RunWith(AndroidJUnit4::class)
class NonDestructiveOpenHelperTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "nondestructive-test.db"
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        context.deleteDatabase(name)
        dbFile = context.getDatabasePath(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun build(factory: SupportSQLiteOpenHelper.Factory?): VireoDatabase {
        val builder = Room.databaseBuilder(context, VireoDatabase::class.java, name)
            .allowMainThreadQueries()
        if (factory != null) builder.openHelperFactory(factory)
        return builder.build()
    }

    private fun createPopulatedDatabase() {
        val db = build(NonDestructiveOpenHelperFactory.default())
        try {
            runBlocking { db.profileDao().insert(ProfileEntity(name = "A")) }
        } finally {
            db.close()
        }
    }

    private fun corruptHeader() {
        RandomAccessFile(dbFile, "rw").use { file ->
            file.seek(0)
            file.write(ByteArray(16))
        }
    }

    @Test
    fun corruptDatabaseIsKeptWithNonDestructiveFactory() {
        createPopulatedDatabase()
        corruptHeader()
        val before = dbFile.readBytes()

        val db = build(NonDestructiveOpenHelperFactory.default())
        try {
            db.openHelper.writableDatabase
            fail("Oczekiwano wyjątku przy uszkodzonej bazie")
        } catch (e: Exception) {
            // oczekiwane
        } finally {
            runCatching { db.close() }
        }

        assertTrue("plik uszkodzonej bazy powinien zostać", dbFile.exists())
        assertArrayEquals(before, dbFile.readBytes())
    }

    @Test
    fun controlDefaultFactoryDeletesCorruptDatabase() {
        createPopulatedDatabase()
        corruptHeader()

        val db = build(null)
        try {
            db.openHelper.writableDatabase
        } catch (e: Exception) {
            // oczekiwane
        } finally {
            runCatching { db.close() }
        }

        assertFalse("domyślna fabryka Room powinna usunąć uszkodzony plik", dbFile.exists())
    }

    @Test
    fun healthyDatabaseWorksWithNonDestructiveFactory() {
        createPopulatedDatabase()

        val db = build(NonDestructiveOpenHelperFactory.default())
        try {
            val count = runBlocking { db.profileDao().count() }
            assertEquals(1, count)
        } finally {
            db.close()
        }
    }

    @Test
    fun onCorruptionDoesNotCallDelegateOrDeleteFile() {
        createPopulatedDatabase()
        var delegateCalled = false
        val delegate = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            override fun onCorruption(db: SupportSQLiteDatabase) {
                delegateCalled = true
            }
        }

        val db = build(NonDestructiveOpenHelperFactory.default())
        try {
            NonDestructiveCallback(delegate).onCorruption(db.openHelper.writableDatabase)
        } finally {
            db.close()
        }

        assertFalse(delegateCalled)
        assertTrue(dbFile.exists())
    }
}
