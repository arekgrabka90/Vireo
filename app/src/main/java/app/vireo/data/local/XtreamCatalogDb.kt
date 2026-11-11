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
    tableName = "xtream_catalog_categories",
    primaryKeys = ["sourceId", "contentType", "categoryId"],
    indices = [
        Index(value = ["sourceId", "contentType", "categoryId"]),
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
data class XtreamCatalogCategoryEntity(
    val sourceId: Long,
    val contentType: String,
    val categoryId: String,
    val name: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "xtream_catalog_items",
    primaryKeys = ["sourceId", "contentType", "itemId"],
    indices = [
        Index(value = ["sourceId", "contentType", "categoryId"]),
        Index(value = ["sourceId", "contentType", "title"]),
        Index(value = ["sourceId", "contentType", "epgChannelId"]),
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
data class XtreamCatalogItemEntity(
    val sourceId: Long,
    val contentType: String,
    val itemId: String,
    val categoryId: String?,
    val title: String,
    val posterUrl: String?,
    val logoUrl: String?,
    val rating: String?,
    val epgChannelId: String?,
    val tmdbId: Int?,
    val imdbId: String?,
    val containerExtension: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "xtream_catalog_sync_state",
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
data class XtreamCatalogSyncStateEntity(
    val sourceId: Long,
    val contentType: String,
    val lastSuccessfulSyncAt: Long,
    val categoryCount: Int,
    val itemCount: Int,
)

@Dao
interface XtreamCatalogDao {

    @Query(
        """
        SELECT * FROM xtream_catalog_categories
        WHERE sourceId = :sourceId AND contentType = :contentType
        ORDER BY name COLLATE NOCASE
        """
    )
    suspend fun getCategories(
        sourceId: Long,
        contentType: String,
    ): List<XtreamCatalogCategoryEntity>

    @Query(
        """
        SELECT * FROM xtream_catalog_items
        WHERE sourceId = :sourceId
          AND contentType = :contentType
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY title COLLATE NOCASE
        """
    )
    suspend fun getItems(
        sourceId: Long,
        contentType: String,
        categoryId: String?,
    ): List<XtreamCatalogItemEntity>

    @Query(
        """
        SELECT COUNT(*) FROM xtream_catalog_categories
        WHERE sourceId = :sourceId AND contentType = :contentType
        """
    )
    suspend fun countCategories(sourceId: Long, contentType: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM xtream_catalog_items
        WHERE sourceId = :sourceId AND contentType = :contentType
        """
    )
    suspend fun countItems(sourceId: Long, contentType: String): Int

    @Query(
        """
        SELECT * FROM xtream_catalog_sync_state
        WHERE sourceId = :sourceId AND contentType = :contentType
        LIMIT 1
        """
    )
    suspend fun getSyncState(
        sourceId: Long,
        contentType: String,
    ): XtreamCatalogSyncStateEntity?

    @Query(
        """
        DELETE FROM xtream_catalog_categories
        WHERE sourceId = :sourceId AND contentType = :contentType
        """
    )
    suspend fun deleteCategories(sourceId: Long, contentType: String)

    @Query(
        """
        DELETE FROM xtream_catalog_items
        WHERE sourceId = :sourceId AND contentType = :contentType
        """
    )
    suspend fun deleteItems(sourceId: Long, contentType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<XtreamCatalogCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<XtreamCatalogItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: XtreamCatalogSyncStateEntity)

    @Transaction
    suspend fun replaceCatalog(
        sourceId: Long,
        contentType: String,
        categories: List<XtreamCatalogCategoryEntity>,
        items: List<XtreamCatalogItemEntity>,
        syncedAt: Long,
    ) {
        deleteCategories(sourceId, contentType)
        deleteItems(sourceId, contentType)

        if (categories.isNotEmpty()) insertCategories(categories)
        if (items.isNotEmpty()) insertItems(items)

        upsertSyncState(
            XtreamCatalogSyncStateEntity(
                sourceId = sourceId,
                contentType = contentType,
                lastSuccessfulSyncAt = syncedAt,
                categoryCount = categories.size,
                itemCount = items.size,
            )
        )
    }
}
