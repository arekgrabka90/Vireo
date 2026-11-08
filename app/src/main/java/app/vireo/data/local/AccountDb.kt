package app.vireo.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val server: String,
    val username: String,
    val password: String,
    @ColumnInfo(defaultValue = "0") val isActive: Boolean = false,
    @ColumnInfo(defaultValue = "'XTREAM'") val providerType: String = "XTREAM",
)

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY isActive DESC, id DESC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): AccountEntity?

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE accounts SET isActive = 1 WHERE id = :id")
    suspend fun activateById(id: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM accounts")
    suspend fun clear()

    @Transaction
    suspend fun insertAndActivate(account: AccountEntity): Long {
        deactivateAll()
        return insert(account.copy(isActive = true))
    }

    @Transaction
    suspend fun activate(id: Long): Boolean {
        if (getById(id) == null) return false
        deactivateAll()
        activateById(id)
        return true
    }

    @Transaction
    suspend fun deleteAndSelectFallback(id: Long): AccountEntity? {
        val wasActive = getActive()?.id == id
        deleteById(id)

        if (wasActive) {
            deactivateAll()
            val fallback = getLatest()
            if (fallback != null) activateById(fallback.id)
        }

        return getActive()
    }

    @Transaction
    suspend fun deleteActiveAndSelectFallback(): AccountEntity? {
        val active = getActive() ?: return null
        return deleteAndSelectFallback(active.id)
    }

    @Transaction
    suspend fun replaceAccount(account: AccountEntity) {
        clear()
        insert(account.copy(isActive = true))
    }
}

@Database(entities = [AccountEntity::class, ProfileEntity::class], version = 4, exportSchema = false)
abstract class VireoDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun profileDao(): ProfileDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE accounts ADD COLUMN isActive INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE accounts ADD COLUMN providerType TEXT NOT NULL DEFAULT 'XTREAM'"
                )
                db.execSQL(
                    "UPDATE accounts SET isActive = 1 " +
                        "WHERE id = (SELECT id FROM accounts ORDER BY id DESC LIMIT 1)"
                )
            }
        }
    }
}
