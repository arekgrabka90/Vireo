package app.vireo.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDeletionCleanupTest {

    private lateinit var db: VireoDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, VireoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deleteWithData_removesLibraryItemsOnlyOfDeletedProfile() {
        runBlocking {
            val profiles = db.profileDao()
            val library = db.libraryDao()
            val first = profiles.insert(ProfileEntity(name = "A"))
            val second = profiles.insert(ProfileEntity(name = "B"))

            library.upsert(item(first, "tmdb:MOVIE:1"))
            library.upsert(item(first, "tmdb:MOVIE:2"))
            library.upsert(item(second, "tmdb:MOVIE:1"))

            profiles.deleteWithData(requireNotNull(profiles.get(first)))

            assertEquals(null, profiles.get(first))
            assertEquals(0, count("library_items", first))
            assertEquals(1, count("library_items", second))
        }
    }

    @Test
    fun deleteWithData_keepsOtherProfilesIntact() {
        runBlocking {
            val profiles = db.profileDao()
            val first = profiles.insert(ProfileEntity(name = "A"))
            val second = profiles.insert(ProfileEntity(name = "B"))

            profiles.deleteWithData(requireNotNull(profiles.get(first)))

            assertEquals(second, profiles.get(second)?.id)
            assertEquals(1, profiles.count())
        }
    }

    @Test
    fun profileDelete_cascadesToTablesWithForeignKeys() {
        runBlocking {
            val profiles = db.profileDao()
            val first = profiles.insert(ProfileEntity(name = "A"))
            val raw = db.openHelper.writableDatabase
            raw.execSQL(
                "INSERT INTO recent_searches (profileId, normalizedQuery, query, createdAt) " +
                    "VALUES ($first, 'x', 'x', 1)"
            )
            raw.execSQL(
                "INSERT INTO live_channel_history " +
                    "(profileId, sourceId, streamId, channelName, lastPlayedAt) " +
                    "VALUES ($first, 1, '10', 'Kanał', 1)"
            )

            profiles.deleteWithData(requireNotNull(profiles.get(first)))

            assertEquals(0, count("recent_searches", first))
            assertEquals(0, count("live_channel_history", first))
        }
    }

    private fun item(profileId: Long, key: String) = LibraryItemEntity(
        profileId = profileId,
        contentKey = key,
        title = "Tytuł",
        contentType = "MOVIE",
        posterUrl = null,
        backdropUrl = null,
        positionMs = 0L,
        durationMs = 0L,
        watchlisted = false,
        watched = false,
        updatedAt = 0L,
    )

    private fun count(table: String, profileId: Long): Int =
        db.query(
            "SELECT COUNT(*) FROM $table WHERE profileId = ?",
            arrayOf<Any?>(profileId),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
