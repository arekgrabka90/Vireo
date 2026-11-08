package app.vireo.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.RoomDatabase

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val server: String,
    val username: String,
    val password: String,
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): AccountEntity?

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("DELETE FROM accounts")
    suspend fun clear()

    @Transaction
    suspend fun replaceAccount(account: AccountEntity) {
        clear()
        insert(account)
    }
}

@Database(entities = [AccountEntity::class, ProfileEntity::class], version = 3, exportSchema = false)
abstract class VireoDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun profileDao(): ProfileDao
}
