package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allowed_folders")
data class AllowedFolderEntity(
    @PrimaryKey
    val folderPath: String,
    val folderName: String
)
