package com.aether.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE isHidden = 0 ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isHidden = 0")
    suspend fun getAll(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VideoEntity)

    @Query("SELECT * FROM videos WHERE isFavorite = 1 AND isHidden = 0 ORDER BY title")
    fun observeFavorites(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPlayedAt > 0 AND isHidden = 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 40): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPositionMs > 4000 AND completed = 0 AND isHidden = 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeContinue(limit: Int = 20): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isHidden = 0 ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 30): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderId = :folderId AND isHidden = 0 ORDER BY title")
    fun observeFolder(folderId: String): Flow<List<VideoEntity>>

    @Query(
        """
        SELECT * FROM videos WHERE isHidden = 0 AND (
            title LIKE '%' || :q || '%' OR folderName LIKE '%' || :q || '%' OR path LIKE '%' || :q || '%'
        ) ORDER BY lastPlayedAt DESC, title ASC
        """,
    )
    fun search(q: String): Flow<List<VideoEntity>>

    @Query("UPDATE videos SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE videos SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Query(
        """
        UPDATE videos SET lastPlayedAt = :at, lastPositionMs = :position, playCount = playCount + :inc,
        completed = :completed WHERE id = :id
        """,
    )
    suspend fun updateProgress(id: String, at: Long, position: Long, completed: Boolean, inc: Int)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM videos WHERE isNetwork = 0 AND id NOT IN (:keep)")
    suspend fun deleteMissingLocal(keep: List<String>)

    @Query("SELECT COUNT(*) FROM videos WHERE isHidden = 0")
    fun observeCount(): Flow<Int>
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isHidden = 0 ORDER BY name")
    fun observeAll(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FolderEntity>)

    @Query("UPDATE folders SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE folders SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Query("DELETE FROM folders")
    suspend fun clear()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun get(id: Long): PlaylistEntity?

    @Insert
    suspend fun insert(entity: PlaylistEntity): Long

    @Update
    suspend fun update(entity: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder")
    suspend fun getItems(playlistId: Long): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeItem(playlistId: Long, videoId: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearItems(playlistId: Long)

    @Transaction
    suspend fun replaceItems(playlistId: Long, videoIds: List<String>) {
        clearItems(playlistId)
        videoIds.forEachIndexed { index, id ->
            upsertItem(PlaylistItemEntity(playlistId, id, index))
        }
    }
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE videoId = :videoId ORDER BY positionMs")
    fun observeForVideo(videoId: String): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("UPDATE bookmarks SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SavedUrlDao {
    @Query("SELECT * FROM saved_urls ORDER BY lastOpenedAt DESC")
    fun observeAll(): Flow<List<SavedUrlEntity>>

    @Insert
    suspend fun insert(entity: SavedUrlEntity): Long

    @Query("DELETE FROM saved_urls WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE saved_urls SET lastOpenedAt = :at WHERE url = :url")
    suspend fun touch(url: String, at: Long)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status, progress = :progress, localUri = :localUri WHERE id = :id")
    suspend fun update(id: Long, status: String, progress: Int, localUri: String?)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit")
    fun observe(limit: Int = 100): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clear()
}
