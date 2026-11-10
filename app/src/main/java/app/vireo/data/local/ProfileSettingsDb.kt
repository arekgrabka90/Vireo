package app.vireo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "profile_settings",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProfileSettingsEntity(
    @PrimaryKey
    val profileId: Long,

    @androidx.room.ColumnInfo(defaultValue = "'en-US'")
    val contentLanguage: String = "en-US",

    @androidx.room.ColumnInfo(defaultValue = "'system'")
    val preferredAudioLanguage: String = "system",

    @androidx.room.ColumnInfo(defaultValue = "'system'")
    val preferredSubtitleLanguage: String = "system",

    @androidx.room.ColumnInfo(defaultValue = "0")
    val subtitlesEnabled: Boolean = false,

    @androidx.room.ColumnInfo(defaultValue = "18")
    val subtitleSizeSp: Int = 18,

    @androidx.room.ColumnInfo(defaultValue = "'system'")
    val themeMode: String = "system",

    @androidx.room.ColumnInfo(defaultValue = "'violet'")
    val accentColor: String = "violet",

    @androidx.room.ColumnInfo(defaultValue = "1")
    val autoplayNext: Boolean = true,

    @androidx.room.ColumnInfo(defaultValue = "'classic'")
    val liveTvHudTechStyle: String = "classic",

    @androidx.room.ColumnInfo(defaultValue = "1")
    val liveTvHudShowEpgDescription: Boolean = true,

    @androidx.room.ColumnInfo(defaultValue = "1")
    val liveTvStartOnLastChannel: Boolean = true,

    @androidx.room.ColumnInfo(defaultValue = "360")
    val epgRefreshIntervalMinutes: Int = 360,

    @androidx.room.ColumnInfo(defaultValue = "1")
    val showWatched: Boolean = true,

    @androidx.room.ColumnInfo(defaultValue = "1")
    val continueWatchingEnabled: Boolean = true,

    @androidx.room.ColumnInfo(defaultValue = "'{}'")
    val addonConfigsJson: String = "{}",
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,

    @androidx.room.ColumnInfo(defaultValue = "'system'")
    val appLanguageTag: String = "system",

    @androidx.room.ColumnInfo(defaultValue = "1")
    val notificationsEnabled: Boolean = true,

    @androidx.room.ColumnInfo(defaultValue = "0")
    val diagnosticsEnabled: Boolean = false,
)

@Dao
interface ProfileSettingsDao {

    @Query("SELECT * FROM profile_settings WHERE profileId = :profileId LIMIT 1")
    suspend fun get(profileId: Long): ProfileSettingsEntity?

    @Query("SELECT * FROM profile_settings WHERE profileId = :profileId LIMIT 1")
    fun observe(profileId: Long): Flow<ProfileSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(settings: ProfileSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ProfileSettingsEntity)

    @Update
    suspend fun update(settings: ProfileSettingsEntity)

    @Query("DELETE FROM profile_settings WHERE profileId = :profileId")
    suspend fun delete(profileId: Long)
}

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(settings: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}
