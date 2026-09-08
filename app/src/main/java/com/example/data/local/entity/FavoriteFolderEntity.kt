package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_folders")
data class FavoriteFolderEntity(
    @PrimaryKey
    val folderPath: String,
    val folderName: String,
    val dateAdded: Long = System.currentTimeMillis()
)
