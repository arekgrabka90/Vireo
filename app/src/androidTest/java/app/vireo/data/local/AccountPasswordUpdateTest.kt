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
class AccountPasswordUpdateTest {

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
    fun updatePasswordChangesOnlyTargetAccount() {
        runBlocking {
            val dao = db.accountDao()
            val first = dao.insert(AccountEntity(name = "A", server = "s1", username = "u1", password = "p1"))
            val second = dao.insert(AccountEntity(name = "B", server = "s2", username = "u2", password = "p2"))

            dao.updatePassword(first, "new")

            assertEquals("new", dao.getById(first)?.password)
            assertEquals("u1", dao.getById(first)?.username)
            assertEquals("p2", dao.getById(second)?.password)
        }
    }

    @Test
    fun updatePasswordKeepsAccountActive() {
        runBlocking {
            val dao = db.accountDao()
            val id = dao.insertAndActivate(
                AccountEntity(name = "A", server = "s1", username = "u1", password = "p1"),
            )

            dao.updatePassword(id, "new")

            assertEquals(id, dao.getActive()?.id)
            assertEquals("new", dao.getActive()?.password)
        }
    }
}
