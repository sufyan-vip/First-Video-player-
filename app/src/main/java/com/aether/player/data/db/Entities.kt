package com.aether.player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "videos", indices = [Index(value = ["uri"], unique = true), Index("folderId")])
data class VideoEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val path: String?,
    val folderId: String?,
    val folderName: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String?,
    val bitrate: Int,
    val isNetwork: Boolean = false,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val lastPositionMs: Long = 0L,
    val completed: Boolean = false,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String?,
    val videoCount: Int,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "videoId"],
    indices = [Index("videoId")],
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val videoId: String,
    val sortOrder: Int,
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val positionMs: Long,
    val title: String,
    val createdAt: Long,
)

@Entity(tableName = "saved_urls")
data class SavedUrlEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val lastOpenedAt: Long,
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val status: String,
    val progress: Int,
    val localUri: String?,
    val createdAt: Long,
    val systemDownloadId: Long = -1L,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val playedAt: Long,
    val positionMs: Long,
    val durationMs: Long,
)
