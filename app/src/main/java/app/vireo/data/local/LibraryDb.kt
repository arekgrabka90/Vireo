package app.vireo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "library_items",
    primaryKeys = ["profileId", "contentKey"],
    indices = [
        Index(value = ["profileId", "updatedAt"]),
        Index(value = ["profileId", "watchlisted"]),
    ],
)
data class LibraryItemEntity(
    val profileId: Long,
    val contentKey: String,
    val title: String,
    val contentType: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val watchlisted: Boolean,
    val watched: Boolean,
    val updatedAt: Long,
)

@Dao
interface LibraryDao {
    @Query(
        """
        SELECT * FROM library_items
        WHERE profileId = :profileId
        ORDER BY updatedAt DESC
        """
    )
    fun observeAll(profileId: Long): Flow<List<LibraryItemEntity>>

    @Query(
        """
        SELECT * FROM library_items
        WHERE profileId = :profileId AND watchlisted = 1
        ORDER BY updatedAt DESC
        """
    )
    fun observeWatchlist(profileId: Long): Flow<List<LibraryItemEntity>>

    @Query(
        """
        SELECT * FROM library_items
        WHERE profileId = :profileId AND watched = 0
          AND positionMs > 0
        ORDER BY updatedAt DESC
        """
    )
    fun observeContinueWatching(profileId: Long): Flow<List<LibraryItemEntity>>

    @Query(
        """
        SELECT * FROM library_items
        WHERE profileId = :profileId AND contentKey = :contentKey
        LIMIT 1
        """
    )
    suspend fun get(
        profileId: Long,
        contentKey: String,
    ): LibraryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LibraryItemEntity)

    @Query(
        """
        DELETE FROM library_items
        WHERE profileId = :profileId AND contentKey = :contentKey
        """
    )
    suspend fun delete(profileId: Long, contentKey: String)
}
