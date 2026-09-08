package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.dao.VideoPlayerDao
import com.example.data.local.entity.AllowedFolderEntity
import com.example.data.local.entity.ExcludedFolderEntity
import com.example.data.local.entity.FavoriteFolderEntity
import com.example.data.local.entity.HiddenVideoEntity
import com.example.data.local.entity.PlayerSettingsEntity
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(
    private val context: Context,
    private val dao: VideoPlayerDao
) {
    val favoriteFoldersFlow: Flow<List<FavoriteFolderEntity>> = dao.getAllFavoriteFolders()
    val excludedFoldersFlow: Flow<List<ExcludedFolderEntity>> = dao.getAllExcludedFolders()
    val allowedFoldersFlow: Flow<List<AllowedFolderEntity>> = dao.getAllAllowedFolders()
    val hiddenVideosFlow: Flow<List<HiddenVideoEntity>> = dao.getAllHiddenVideos()
    val playerSettingsFlow: Flow<PlayerSettingsEntity?> = dao.getPlayerSettings()

    // Sample fallback videos for zero-state or preview
    private val sampleVideos = listOf(
        VideoItem(
            id = 100001L,
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            path = "/storage/emulated/0/Movies/BigBuckBunny.mp4",
            title = "Big Buck Bunny (Animation)",
            displayName = "BigBuckBunny.mp4",
            duration = 596000L,
            size = 158008374L,
            dateModified = System.currentTimeMillis() / 1000 - 86400 * 2,
            bucketId = "sample_movies",
            bucketDisplayName = "Film & Animasi",
            folderPath = "/storage/emulated/0/Movies",
            resolution = "1920x1080",
            isSample = true
        ),
        VideoItem(
            id = 100002L,
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
            path = "/storage/emulated/0/Movies/ElephantsDream.mp4",
            title = "Elephants Dream (Bass Test)",
            displayName = "ElephantsDream.mp4",
            duration = 653000L,
            size = 140147483L,
            dateModified = System.currentTimeMillis() / 1000 - 86400 * 5,
            bucketId = "sample_movies",
            bucketDisplayName = "Film & Animasi",
            folderPath = "/storage/emulated/0/Movies",
            resolution = "1920x1080",
            isSample = true
        ),
        VideoItem(
            id = 100003L,
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
            path = "/storage/emulated/0/Music/TearsOfSteel_SciFi.mp4",
            title = "Tears of Steel (Sci-Fi / Audio HiFi)",
            displayName = "TearsOfSteel_SciFi.mp4",
            duration = 734000L,
            size = 228000000L,
            dateModified = System.currentTimeMillis() / 1000 - 86400 * 1,
            bucketId = "sample_music",
            bucketDisplayName = "Video Musik HD",
            folderPath = "/storage/emulated/0/Music",
            resolution = "1920x1080",
            isSample = true
        ),
        VideoItem(
            id = 100004L,
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            path = "/storage/emulated/0/Downloads/BiggerBlazes.mp4",
            title = "Nature & Soundscape Video",
            displayName = "BiggerBlazes.mp4",
            duration = 15000L,
            size = 15000000L,
            dateModified = System.currentTimeMillis() / 1000 - 3600 * 4,
            bucketId = "sample_downloads",
            bucketDisplayName = "Unduhan",
            folderPath = "/storage/emulated/0/Downloads",
            resolution = "1280x720",
            isSample = true
        )
    )

    suspend fun getSettings(): PlayerSettingsEntity {
        return dao.getPlayerSettingsSync() ?: PlayerSettingsEntity().also {
            dao.savePlayerSettings(it)
        }
    }

    suspend fun updateSettings(settings: PlayerSettingsEntity) {
        dao.savePlayerSettings(settings)
    }

    suspend fun toggleFavoriteFolder(folderPath: String, folderName: String) {
        if (dao.isFolderFavorite(folderPath)) {
            dao.deleteFavoriteFolder(folderPath)
        } else {
            dao.insertFavoriteFolder(FavoriteFolderEntity(folderPath, folderName))
        }
    }

    suspend fun excludeFolder(folderPath: String, folderName: String) {
        dao.insertExcludedFolder(ExcludedFolderEntity(folderPath, folderName))
    }

    suspend fun removeExcludedFolder(folderPath: String) {
        dao.deleteExcludedFolder(folderPath)
    }

    suspend fun hideVideoFromList(video: VideoItem) {
        val key = if (video.path.isNotBlank()) video.path else video.id.toString()
        dao.insertHiddenVideo(HiddenVideoEntity(key, video.title))
    }

    suspend fun restoreAllHiddenVideos() {
        dao.restoreAllHiddenVideos()
    }

    suspend fun setAllowedFolders(folders: List<Pair<String, String>>) {
        dao.clearAllowedFolders()
        folders.forEach { (path, name) ->
            dao.insertAllowedFolder(AllowedFolderEntity(path, name))
        }
    }

    suspend fun scanVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val excludedFolders = dao.getAllExcludedFolders().first().map { it.folderPath.lowercase() }.toSet()
        val hiddenVideos = dao.getAllHiddenVideos().first().map { it.videoPathOrId.lowercase() }.toSet()
        val settings = getSettings()
        val allowedFolders = if (settings.whitelistModeEnabled) {
            dao.getAllAllowedFolders().first().map { it.folderPath.lowercase() }.toSet()
        } else {
            null
        }

        val videos = mutableListOf<VideoItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dataCol = c.getColumnIndex(MediaStore.Video.Media.DATA)
                val titleCol = c.getColumnIndex(MediaStore.Video.Media.TITLE)
                val nameCol = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = c.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketIdCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val widthCol = c.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = c.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val path = if (dataCol != -1) c.getString(dataCol) ?: "" else ""
                    val title = if (titleCol != -1) c.getString(titleCol) ?: "Video $id" else "Video $id"
                    val displayName = if (nameCol != -1) c.getString(nameCol) ?: title else title
                    val duration = if (durCol != -1) c.getLong(durCol) else 0L
                    val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L
                    val dateModified = if (dateCol != -1) c.getLong(dateCol) else 0L
                    val bucketId = if (bucketIdCol != -1) c.getString(bucketIdCol) ?: "default" else "default"
                    val bucketName = if (bucketNameCol != -1) c.getString(bucketNameCol) ?: "Internal" else "Internal"
                    val width = if (widthCol != -1) c.getInt(widthCol) else 0
                    val height = if (heightCol != -1) c.getInt(heightCol) else 0

                    val folderPath = if (path.isNotBlank()) {
                        File(path).parent ?: "/storage/emulated/0/$bucketName"
                    } else {
                        "/storage/emulated/0/$bucketName"
                    }

                    // Check if hidden by user (removed from list)
                    if (hiddenVideos.contains(path.lowercase()) || hiddenVideos.contains(id.toString().lowercase())) {
                        continue
                    }

                    // Check if folder is excluded by user (privacy protection)
                    val folderLower = folderPath.lowercase()
                    val bucketLower = bucketName.lowercase()
                    val isExcluded = excludedFolders.any { excluded ->
                        folderLower.startsWith(excluded) || folderLower.contains(excluded) || bucketLower == excluded
                    }
                    if (isExcluded) {
                        continue
                    }

                    // Check whitelist mode if active
                    if (allowedFolders != null && allowedFolders.isNotEmpty()) {
                        val isAllowed = allowedFolders.any { allowed ->
                            folderLower.startsWith(allowed) || bucketLower == allowed
                        }
                        if (!isAllowed) continue
                    }

                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val resString = if (width > 0 && height > 0) "${width}x${height}" else ""

                    videos.add(
                        VideoItem(
                            id = id,
                            uri = contentUri,
                            path = path,
                            title = title,
                            displayName = displayName,
                            duration = duration,
                            size = size,
                            dateModified = dateModified,
                            bucketId = bucketId,
                            bucketDisplayName = bucketName,
                            folderPath = folderPath,
                            resolution = resString,
                            isSample = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If no local videos found on device/emulator, include sample videos
        if (videos.isEmpty()) {
            val filteredSamples = sampleVideos.filter { sample ->
                val notHidden = !hiddenVideos.contains(sample.path.lowercase()) && !hiddenVideos.contains(sample.id.toString())
                val notExcluded = !excludedFolders.any { excluded -> sample.folderPath.lowercase().contains(excluded) }
                notHidden && notExcluded
            }
            videos.addAll(filteredSamples)
        }

        videos
    }

    suspend fun getFolders(videos: List<VideoItem>): List<FolderItem> = withContext(Dispatchers.Default) {
        val favorites = dao.getAllFavoriteFolders().first().map { it.folderPath.lowercase() }.toSet()
        val excluded = dao.getAllExcludedFolders().first().map { it.folderPath.lowercase() }.toSet()

        val grouped = videos.groupBy { it.folderPath.ifBlank { it.bucketDisplayName } }

        grouped.map { (path, vList) ->
            val firstVid = vList.firstOrNull()
            val folderName = firstVid?.bucketDisplayName ?: File(path).name
            val totalSize = vList.sumOf { it.size }
            val latestDate = vList.maxOfOrNull { it.dateModified } ?: 0L
            val isFav = favorites.contains(path.lowercase()) || favorites.contains(folderName.lowercase())
            val isEx = excluded.contains(path.lowercase()) || excluded.contains(folderName.lowercase())

            FolderItem(
                bucketId = firstVid?.bucketId ?: path,
                name = folderName,
                path = path,
                videoCount = vList.size,
                totalSize = totalSize,
                latestDate = latestDate,
                firstVideoUri = firstVid?.uri?.toString(),
                isFavorite = isFav,
                isExcluded = isEx
            )
        }
    }

    fun sortVideos(videos: List<VideoItem>, order: VideoSortOrder): List<VideoItem> {
        return when (order) {
            VideoSortOrder.DATE_DESC -> videos.sortedByDescending { it.dateModified }
            VideoSortOrder.DATE_ASC -> videos.sortedBy { it.dateModified }
            VideoSortOrder.SIZE_DESC -> videos.sortedByDescending { it.size }
            VideoSortOrder.SIZE_ASC -> videos.sortedBy { it.size }
            VideoSortOrder.NAME_ASC -> videos.sortedBy { it.title.lowercase() }
            VideoSortOrder.NAME_DESC -> videos.sortedByDescending { it.title.lowercase() }
        }
    }

    fun sortFolders(folders: List<FolderItem>, order: VideoSortOrder): List<FolderItem> {
        return when (order) {
            VideoSortOrder.DATE_DESC -> folders.sortedByDescending { it.latestDate }
            VideoSortOrder.DATE_ASC -> folders.sortedBy { it.latestDate }
            VideoSortOrder.SIZE_DESC -> folders.sortedByDescending { it.totalSize }
            VideoSortOrder.SIZE_ASC -> folders.sortedBy { it.totalSize }
            VideoSortOrder.NAME_ASC -> folders.sortedBy { it.name.lowercase() }
            VideoSortOrder.NAME_DESC -> folders.sortedByDescending { it.name.lowercase() }
        }
    }
}
