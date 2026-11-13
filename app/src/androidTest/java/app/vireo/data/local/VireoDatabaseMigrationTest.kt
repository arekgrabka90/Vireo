package app.vireo.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VireoDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VireoDatabase::class.java,
    )

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun createV14DatabaseFromExportedSchema_createsAllExpectedTables() {
        val db = helper.createDatabase(TEST_DB, 14)
        try {
            val tables = mutableSetOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                while (cursor.moveToNext()) {
                    tables += cursor.getString(0)
                }
            }

            val expectedTables = setOf(
                "accounts",
                "profiles",
                "xtream_content_index",
                "catalog_sync_state",
                "library_items",
                "xtream_catalog_categories",
                "xtream_catalog_items",
                "xtream_catalog_sync_state",
                "epg_programs",
                "epg_sync_state",
                "profile_settings",
                "app_settings",
                "recent_searches",
                "live_channel_history",
            )
            assertTrue(
                "Missing expected tables: ${expectedTables - tables}",
                tables.containsAll(expectedTables),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun openVireoDatabase_matchesExportedV14Schema() {
        helper.createDatabase(TEST_DB, 14).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val roomDb = Room.databaseBuilder(context, VireoDatabase::class.java, TEST_DB)
            .allowMainThreadQueries()
            .build()
        try {
            // Otwarcie przez Room uruchamia walidację identityHash wyeksportowanego
            // schematu. Niezgodność schematu i kodu kończy się IllegalStateException.
            val version = roomDb.query("PRAGMA user_version", null).use { cursor ->
                assertTrue("PRAGMA user_version zwróciło pusty wynik", cursor.moveToFirst())
                cursor.getInt(0)
            }
            assertEquals(14, version)

            val accountCount = roomDb.query("SELECT COUNT(*) FROM accounts", null).use { cursor ->
                assertTrue("COUNT(*) zwróciło pusty wynik", cursor.moveToFirst())
                cursor.getInt(0)
            }
            assertEquals(0, accountCount)
        } finally {
            roomDb.close()
        }
    }

    private companion object {
        const val TEST_DB = "vireo-migration-test.db"
    }
}
