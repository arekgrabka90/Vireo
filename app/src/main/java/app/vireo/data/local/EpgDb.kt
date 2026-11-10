package app.vireo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Entity(
    tableName = "epg_programs",
    primaryKeys = ["sourceId", "streamId", "startTimeMs"],
    indices = [
        Index(value = ["sourceId", "streamId", "startTimeMs"]),
        Index(value = ["sourceId", "streamId", "endTimeMs"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EpgProgramEntity(
    val sourceId: Long,
    val streamId: String,
    val title: String,
    val description: String?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val category: String?,
    val iconUrl: String?,
)

@Entity(
    tableName = "epg_sync_state",
    primaryKeys = ["sourceId", "streamId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EpgSyncStateEntity(
    val sourceId: Long,
    val streamId: String,
    val lastSuccessfulSyncAt: Long,
)

@Dao
interface EpgDao {

    @Query(
        """
        SELECT * FROM epg_programs
        WHERE sourceId = :sourceId
          AND streamId = :streamId
          AND startTimeMs < :toTimeMs
          AND endTimeMs > :fromTimeMs
        ORDER BY startTimeMs
        """
    )
    suspend fun getPrograms(
        sourceId: Long,
        streamId: String,
        fromTimeMs: Long,
        toTimeMs: Long,
    ): List<EpgProgramEntity>

    @Query(
        """
        SELECT * FROM epg_sync_state
        WHERE sourceId = :sourceId AND streamId = :streamId
        LIMIT 1
        """
    )
    suspend fun getSyncState(
        sourceId: Long,
        streamId: String,
    ): EpgSyncStateEntity?

    @Query(
        "DELETE FROM epg_programs WHERE sourceId = :sourceId AND streamId = :streamId"
    )
    suspend fun deletePrograms(sourceId: Long, streamId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(items: List<EpgProgramEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: EpgSyncStateEntity)

    @Query("DELETE FROM epg_programs WHERE endTimeMs < :cutoffMs")
    suspend fun deleteExpired(cutoffMs: Long)

    @Transaction
    suspend fun replaceSchedule(
        sourceId: Long,
        streamId: String,
        programs: List<EpgProgramEntity>,
        syncedAt: Long,
    ) {
        deletePrograms(sourceId, streamId)
        if (programs.isNotEmpty()) insertPrograms(programs)
        upsertSyncState(
            EpgSyncStateEntity(
                sourceId = sourceId,
                streamId = streamId,
                lastSuccessfulSyncAt = syncedAt,
            )
        )
    }
}
