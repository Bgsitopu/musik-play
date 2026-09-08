package com.example.data.model

data class FolderItem(
    val bucketId: String,
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalSize: Long,
    val latestDate: Long,
    val firstVideoUri: String?,
    val isFavorite: Boolean = false,
    val isExcluded: Boolean = false
) {
    val formattedTotalSize: String
        get() {
            if (totalSize <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(totalSize.toDouble()) / Math.log10(1024.0)).toInt()
            val safeIndex = digitGroups.coerceIn(0, units.size - 1)
            val value = totalSize / Math.pow(1024.0, safeIndex.toDouble())
            return String.format("%.1f %s", value, units[safeIndex])
        }
}

enum class VideoSortOrder {
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC,
    NAME_ASC,
    NAME_DESC
}
