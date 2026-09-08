package com.example.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val title: String,
    val displayName: String,
    val duration: Long, // in milliseconds
    val size: Long, // in bytes
    val dateModified: Long, // epoch seconds or millis
    val bucketId: String,
    val bucketDisplayName: String,
    val folderPath: String,
    val resolution: String = "",
    val isSample: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            val safeIndex = digitGroups.coerceIn(0, units.size - 1)
            val value = size / Math.pow(1024.0, safeIndex.toDouble())
            return String.format("%.1f %s", value, units[safeIndex])
        }
}
