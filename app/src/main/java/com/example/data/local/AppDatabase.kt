package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.VideoPlayerDao
import com.example.data.local.entity.AllowedFolderEntity
import com.example.data.local.entity.ExcludedFolderEntity
import com.example.data.local.entity.FavoriteFolderEntity
import com.example.data.local.entity.HiddenVideoEntity
import com.example.data.local.entity.PlayerSettingsEntity

@Database(
    entities = [
        FavoriteFolderEntity::class,
        ExcludedFolderEntity::class,
        AllowedFolderEntity::class,
        HiddenVideoEntity::class,
        PlayerSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoPlayerDao(): VideoPlayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_player_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
