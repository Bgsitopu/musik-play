package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AllowedFolderEntity
import com.example.data.local.entity.ExcludedFolderEntity
import com.example.data.local.entity.FavoriteFolderEntity
import com.example.data.local.entity.HiddenVideoEntity
import com.example.data.local.entity.PlayerSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoPlayerDao {
    // Favorite folders
    @Query("SELECT * FROM favorite_folders ORDER BY dateAdded DESC")
    fun getAllFavoriteFolders(): Flow<List<FavoriteFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteFolder(folder: FavoriteFolderEntity)

    @Query("DELETE FROM favorite_folders WHERE folderPath = :folderPath")
    suspend fun deleteFavoriteFolder(folderPath: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_folders WHERE folderPath = :folderPath)")
    suspend fun isFolderFavorite(folderPath: String): Boolean

    // Excluded folders (privacy / don't scan)
    @Query("SELECT * FROM excluded_folders ORDER BY dateAdded DESC")
    fun getAllExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcludedFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun deleteExcludedFolder(folderPath: String)

    // Allowed folders (whitelist)
    @Query("SELECT * FROM allowed_folders")
    fun getAllAllowedFolders(): Flow<List<AllowedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllowedFolder(folder: AllowedFolderEntity)

    @Query("DELETE FROM allowed_folders WHERE folderPath = :folderPath")
    suspend fun deleteAllowedFolder(folderPath: String)

    @Query("DELETE FROM allowed_folders")
    suspend fun clearAllowedFolders()

    // Hidden videos (removed from list only, file untouched)
    @Query("SELECT * FROM hidden_videos")
    fun getAllHiddenVideos(): Flow<List<HiddenVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenVideo(video: HiddenVideoEntity)

    @Query("DELETE FROM hidden_videos WHERE videoPathOrId = :pathOrId")
    suspend fun deleteHiddenVideo(pathOrId: String)

    @Query("DELETE FROM hidden_videos")
    suspend fun restoreAllHiddenVideos()

    // Settings
    @Query("SELECT * FROM player_settings WHERE id = 1 LIMIT 1")
    fun getPlayerSettings(): Flow<PlayerSettingsEntity?>

    @Query("SELECT * FROM player_settings WHERE id = 1 LIMIT 1")
    suspend fun getPlayerSettingsSync(): PlayerSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayerSettings(settings: PlayerSettingsEntity)
}
