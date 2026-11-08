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
    tableName = "xtream_content_index",
    primaryKeys = ["sourceId", "contentType", "streamId"],
    indices = [
        Index(value = ["sourceId", "contentType"]),
        Index(value = ["normalizedTitle", "year", "contentType"]),
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
data class XtreamContentIndexEntity(
    val sourceId: Long,
    val contentType: String,
    val streamId: String,
    val title: String,
    val normalizedTitle: String,
    val year: Int?,
    val posterUrl: String?,
    val categoryId: String?,
    val indexedAt: Long,
)

@Entity(
    tableName = "catalog_sync_state",
    primaryKeys = ["sourceId", "contentType"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CatalogSyncStateEntity(
    val sourceId: Long,
    val contentType: String,
    val lastSuccessfulSyncAt: Long,
    val itemCount: Int,
)

@Dao
interface XtreamContentIndexDao {

    @Query(
        """
        SELECT * FROM xtream_content_index
        WHERE sourceId = :sourceId AND contentType = :contentType
        ORDER BY title COLLATE NOCASE
        """
    )
    suspend fun getForSource(
        sourceId: Long,
        contentType: String,
    ): List<XtreamContentIndexEntity>

    @Query(
        """
        SELECT * FROM xtream_content_index
        WHERE contentType = :contentType
          AND normalizedTitle = :normalizedTitle
          AND (:year IS NULL OR year = :year)
        ORDER BY sourceId
        """
    )
    suspend fun findCandidates(
        contentType: String,
        normalizedTitle: String,
        year: Int?,
    ): List<XtreamContentIndexEntity>

    @Query(
        """
        SELECT * FROM catalog_sync_state
        WHERE sourceId = :sourceId AND contentType = :contentType
        LIMIT 1
        """
    )
    suspend fun getSyncState(
        sourceId: Long,
        contentType: String,
    ): CatalogSyncStateEntity?

    @Query(
        """
        DELETE FROM xtream_content_index
        WHERE sourceId = :sourceId AND contentType = :contentType
        """
    )
    suspend fun deleteForSourceAndType(
        sourceId: Long,
        contentType: String,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<XtreamContentIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: CatalogSyncStateEntity)

    @Transaction
    suspend fun replaceForSourceAndType(
        sourceId: Long,
        contentType: String,
        items: List<XtreamContentIndexEntity>,
        syncedAt: Long,
    ) {
        deleteForSourceAndType(sourceId, contentType)

        if (items.isNotEmpty()) {
            insertAll(items)
        }

        upsertSyncState(
            CatalogSyncStateEntity(
                sourceId = sourceId,
                contentType = contentType,
                lastSuccessfulSyncAt = syncedAt,
                itemCount = items.size,
            )
        )
    }
}
