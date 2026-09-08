package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_settings")
data class PlayerSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isDarkMode: Boolean = true, // Elegant dark mode by default
    val ignoreAudioFocus: Boolean = true, // Concurrent playback without interruption by default
    val backgroundPlaybackEnabled: Boolean = true, // Play in background like music
    val videoVolume: Float = 1.0f, // Independent video volume (0.0 to 1.0 or boosted)
    val folderSortOrder: String = "DATE_DESC", // Automatically sort by date or size
    val whitelistModeEnabled: Boolean = false, // Whitelist folders
    val equalizerEnabled: Boolean = true,
    val bassStrength: Int = 300, // 0 to 1000
    val trebleStrength: Int = 200, // -1000 to 1000
    val eqPreset: String = "Normal",
    val customBandLevels: String = "0,0,0,0,0" // comma-separated dB millibels
)
