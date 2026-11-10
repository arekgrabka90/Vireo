package app.vireo.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.OnConflictStrategy
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

@Database(
    entities = [
        AccountEntity::class,
        ProfileEntity::class,
        XtreamContentIndexEntity::class,
        CatalogSyncStateEntity::class,
        LibraryItemEntity::class,
        XtreamCatalogCategoryEntity::class,
        XtreamCatalogItemEntity::class,
        XtreamCatalogSyncStateEntity::class,
        EpgProgramEntity::class,
        EpgSyncStateEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class VireoDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun profileDao(): ProfileDao
    abstract fun xtreamContentIndexDao(): XtreamContentIndexDao
    abstract fun libraryDao(): LibraryDao
    abstract fun xtreamCatalogDao(): XtreamCatalogDao
    abstract fun epgDao(): EpgDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS xtream_content_index (
                        sourceId INTEGER NOT NULL,
                        contentType TEXT NOT NULL,
                        streamId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        normalizedTitle TEXT NOT NULL,
                        year INTEGER,
                        posterUrl TEXT,
                        categoryId TEXT,
                        indexedAt INTEGER NOT NULL,
                        PRIMARY KEY(sourceId, contentType, streamId),
                        FOREIGN KEY(sourceId) REFERENCES accounts(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_xtream_content_index_sourceId_contentType " +
                        "ON xtream_content_index(sourceId, contentType)"
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_xtream_content_index_normalizedTitle_year_contentType " +
                        "ON xtream_content_index(normalizedTitle, year, contentType)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS catalog_sync_state (
                        sourceId INTEGER NOT NULL,
                        contentType TEXT NOT NULL,
                        lastSuccessfulSyncAt INTEGER NOT NULL,
                        itemCount INTEGER NOT NULL,
                        PRIMARY KEY(sourceId, contentType),
                        FOREIGN KEY(sourceId) REFERENCES accounts(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS library_items (
                        profileId INTEGER NOT NULL,
                        contentKey TEXT NOT NULL,
                        title TEXT NOT NULL,
                        contentType TEXT NOT NULL,
                        posterUrl TEXT,
                        backdropUrl TEXT,
                        positionMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        watchlisted INTEGER NOT NULL,
                        watched INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(profileId, contentKey)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_library_items_profileId_updatedAt " +
                        "ON library_items(profileId, updatedAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_library_items_profileId_watchlisted " +
                        "ON library_items(profileId, watchlisted)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE xtream_content_index ADD COLUMN tmdbId INTEGER")
                db.execSQL("ALTER TABLE xtream_content_index ADD COLUMN imdbId TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_xtream_content_index_tmdbId_contentType " +
                        "ON xtream_content_index(tmdbId, contentType)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_xtream_content_index_imdbId_contentType " +
                        "ON xtream_content_index(imdbId, contentType)"
                )
                db.execSQL("DELETE FROM catalog_sync_state")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS xtream_catalog_categories (
                        sourceId INTEGER NOT NULL,
                        contentType TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(sourceId, contentType, categoryId),
                        FOREIGN KEY(sourceId) REFERENCES accounts(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_xtream_catalog_categories_sourceId_contentType_categoryId " +
                        "ON xtream_catalog_categories(sourceId, contentType, categoryId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS xtream_catalog_items (
                        sourceId INTEGER NOT NULL,
                        contentType TEXT NOT NULL,
                        itemId TEXT NOT NULL,
                        categoryId TEXT,
                        title TEXT NOT NULL,
                        posterUrl TEXT,
                        rating TEXT,
                        epgChannelId TEXT,
                        tmdbId INTEGER,
                        imdbId TEXT,
                        containerExtension TEXT,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(sourceId, contentType, itemId),
                        FOREIGN KEY(sourceId) REFERENCES accounts(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_xtream_catalog_items_sourceId_contentType_categoryId " +
                        "ON xtream_catalog_items(sourceId, contentType, categoryId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_xtream_catalog_items_sourceId_contentType_title " +
                        "ON xtream_catalog_items(sourceId, contentType, title)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_xtream_catalog_items_sourceId_contentType_epgChannelId " +
                        "ON xtream_catalog_items(sourceId, contentType, epgChannelId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS xtream_catalog_sync_state (
                        sourceId INTEGER NOT NULL,
                        contentType TEXT NOT NULL,
                        lastSuccessfulSyncAt INTEGER NOT NULL,
                        categoryCount INTEGER NOT NULL,
                        itemCount INTEGER NOT NULL,
                        PRIMARY KEY(sourceId, contentType),
                        FOREIGN KEY(sourceId) REFERENCES accounts(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS epg_programs (
                        sourceId INTEGER NOT NULL,
                        streamId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        startTimeMs INTEGER NOT NULL,
                        endTimeMs INTEGER NOT NULL,
                        category TEXT,
                        iconUrl TEXT,
                        PRIMARY KEY(sourceId, streamId, startTimeMs),
                        FOREIGN KEY(sourceId) REFERENCES accounts(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_epg_programs_sourceId_streamId_startTimeMs " +
                        "ON epg_programs(sourceId, streamId, startTimeMs)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_epg_programs_sourceId_streamId_endTimeMs " +
                        "ON epg_programs(sourceId, streamId, endTimeMs)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS epg_sync_state (
                        sourceId INTEGER NOT NULL,
                        streamId TEXT NOT NULL,
                        lastSuccessfulSyncAt INTEGER NOT NULL,
                        PRIMARY KEY(sourceId, streamId),
                        FOREIGN KEY(sourceId) REFERENCES accounts(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE epg_sync_state " +
                        "ADD COLUMN lastAttemptAt INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE epg_sync_state " +
                        "SET lastAttemptAt = lastSuccessfulSyncAt"
                )
            }
        }


    }
}
