package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_videos")
data class HiddenVideoEntity(
    @PrimaryKey
    val videoPathOrId: String,
    val title: String,
    val dateHidden: Long = System.currentTimeMillis()
)
