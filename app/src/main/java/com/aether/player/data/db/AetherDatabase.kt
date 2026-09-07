package com.aether.player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoEntity::class,
        FolderEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        BookmarkEntity::class,
        SavedUrlEntity::class,
        DownloadEntity::class,
        HistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun videos(): VideoDao
    abstract fun folders(): FolderDao
    abstract fun playlists(): PlaylistDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun savedUrls(): SavedUrlDao
    abstract fun downloads(): DownloadDao
    abstract fun history(): HistoryDao

    companion object {
        fun create(context: Context): AetherDatabase =
            Room.databaseBuilder(context, AetherDatabase::class.java, "aether.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
