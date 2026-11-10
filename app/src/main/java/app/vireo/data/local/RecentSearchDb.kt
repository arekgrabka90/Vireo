package app.vireo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "recent_searches",
    primaryKeys = ["profileId", "normalizedQuery"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["profileId", "createdAt"]),
    ],
)
data class RecentSearchEntity(
    val profileId: Long,
    val normalizedQuery: String,
    val query: String,
    val createdAt: Long,
)

@Dao
interface RecentSearchDao {

    @Query(
        """
        SELECT * FROM recent_searches
        WHERE profileId = :profileId
        ORDER BY createdAt DESC
        LIMIT 8
        """
    )
    fun observe(profileId: Long): Flow<List<RecentSearchEntity>>

    @Query(
        """
        DELETE FROM recent_searches
        WHERE profileId = :profileId
          AND normalizedQuery = :normalizedQuery
        """
    )
    suspend fun deleteDuplicate(profileId: Long, normalizedQuery: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecentSearchEntity)

    @Query(
        """
        DELETE FROM recent_searches
        WHERE profileId = :profileId
          AND normalizedQuery NOT IN (
              SELECT normalizedQuery
              FROM recent_searches
              WHERE profileId = :profileId
              ORDER BY createdAt DESC
              LIMIT 8
          )
        """
    )
    suspend fun trim(profileId: Long)

    @Query("DELETE FROM recent_searches WHERE profileId = :profileId")
    suspend fun clear(profileId: Long)

    @Transaction
    suspend fun add(item: RecentSearchEntity) {
        deleteDuplicate(item.profileId, item.normalizedQuery)
        insert(item)
        trim(item.profileId)
    }
}
