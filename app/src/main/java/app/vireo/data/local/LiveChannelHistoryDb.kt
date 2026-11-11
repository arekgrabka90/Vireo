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
    tableName = "live_channel_history",
    primaryKeys = ["profileId", "sourceId", "streamId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["profileId", "lastPlayedAt"])],
)
data class LiveChannelHistoryEntity(
    val profileId: Long,
    val sourceId: Long,
    val streamId: String,
    val channelName: String,
    val epgChannelId: String?,
    val logoUrl: String?,
    val lastPlayedAt: Long,
)

@Dao
interface LiveChannelHistoryDao {
    @Query(
        """SELECT * FROM live_channel_history
           WHERE profileId = :profileId
           ORDER BY lastPlayedAt DESC LIMIT 12"""
    )
    fun observe(profileId: Long): Flow<List<LiveChannelHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LiveChannelHistoryEntity)

    @Query(
        """DELETE FROM live_channel_history
           WHERE profileId = :profileId
             AND rowid NOT IN (
                 SELECT rowid FROM live_channel_history
                 WHERE profileId = :profileId
                 ORDER BY lastPlayedAt DESC LIMIT 12
             )"""
    )
    suspend fun trim(profileId: Long)

    @Transaction
    suspend fun record(item: LiveChannelHistoryEntity) {
        insert(item)
        trim(item.profileId)
    }
}
