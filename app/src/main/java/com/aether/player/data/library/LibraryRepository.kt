package com.aether.player.data.library

import android.net.Uri
import com.aether.player.data.db.AetherDatabase
import com.aether.player.data.db.BookmarkEntity
import com.aether.player.data.db.DownloadEntity
import com.aether.player.data.db.FolderEntity
import com.aether.player.data.db.HistoryEntity
import com.aether.player.data.db.PlaylistEntity
import com.aether.player.data.db.SavedUrlEntity
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.prefs.SortMode
import com.aether.player.domain.WatchProgressLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

class LibraryRepository(
    private val db: AetherDatabase,
    private val scanner: MediaStoreScanner,
) {
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning
    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError

    fun videos(): Flow<List<VideoEntity>> = db.videos().observeAll()
    fun hiddenVideos(): Flow<List<VideoEntity>> = db.videos().observeHidden()
    fun folders(): Flow<List<FolderEntity>> = db.folders().observeAll()
    fun favorites(): Flow<List<VideoEntity>> = db.videos().observeFavorites()
    fun recent(): Flow<List<VideoEntity>> = db.videos().observeRecent()
    fun continueWatching(): Flow<List<VideoEntity>> = db.videos().observeContinue()
    fun recentlyAdded(): Flow<List<VideoEntity>> = db.videos().observeRecentlyAdded()
    fun folderVideos(id: String): Flow<List<VideoEntity>> = db.videos().observeFolder(id)
    fun search(q: String): Flow<List<VideoEntity>> = db.videos().search(q)
    fun playlists() = db.playlists().observeAll()
    fun playlistItems(id: Long) = db.playlists().observeItems(id)
    fun bookmarks(videoId: String) = db.bookmarks().observeForVideo(videoId)
    fun savedUrls() = db.savedUrls().observeAll()
    fun downloads() = db.downloads().observeAll()
    fun history() = db.history().observe()
    fun videoCount() = db.videos().observeCount()

    suspend fun getVideo(id: String) = db.videos().getById(id)

    suspend fun scanLibrary(excluded: Set<String> = emptySet(), hideDotFiles: Boolean = false) {
        _scanning.value = true
        _scanError.value = null
        try {
            val (videos, folders) = scanner.scan(excluded, hideDotFiles)
            val existing = db.videos().getAll().associateBy { it.id }
            val merged = videos.map { fresh ->
                val old = existing[fresh.id]
                if (old == null) fresh else fresh.copy(
                    isFavorite = old.isFavorite,
                    isHidden = old.isHidden,
                    playCount = old.playCount,
                    lastPlayedAt = old.lastPlayedAt,
                    lastPositionMs = old.lastPositionMs,
                    completed = old.completed,
                )
            }
            db.videos().upsertAll(merged)
            val keep = merged.map { it.id }
            if (keep.isNotEmpty()) {
                db.videos().deleteMissingLocal(keep)
            }
            db.folders().clear()
            db.folders().upsertAll(folders)
        } catch (t: Throwable) {
            _scanError.value = t.message ?: "Library scan failed"
        } finally {
            _scanning.value = false
        }
    }

    suspend fun toggleFavorite(id: String) {
        val v = db.videos().getById(id) ?: return
        db.videos().setFavorite(id, !v.isFavorite)
    }

    suspend fun setHidden(id: String, hidden: Boolean) = db.videos().setHidden(id, hidden)

    suspend fun markWatched(id: String, watched: Boolean) {
        db.videos().updateProgress(id, System.currentTimeMillis(), 0L, watched, 0)
    }

    suspend fun recordPlayback(
        video: VideoEntity,
        positionMs: Long,
        incrementPlay: Boolean,
        historyEnabled: Boolean,
        incognito: Boolean,
        threshold: Float,
    ) {
        if (incognito) return
        val completed = WatchProgressLogic.isCompleted(positionMs, video.durationMs, threshold)
        db.videos().updateProgress(
            id = video.id,
            at = System.currentTimeMillis(),
            position = if (completed) 0L else positionMs,
            completed = completed,
            inc = if (incrementPlay) 1 else 0,
        )
        if (historyEnabled) {
            db.history().insert(
                HistoryEntity(
                    videoId = video.id,
                    playedAt = System.currentTimeMillis(),
                    positionMs = positionMs,
                    durationMs = video.durationMs,
                ),
            )
        }
    }

    suspend fun upsertNetworkVideo(url: String, title: String): VideoEntity {
        val existing = db.videos().getByUri(url)
        if (existing != null) return existing
        val entity = VideoEntity(
            id = "net-${url.hashCode()}-${System.currentTimeMillis()}",
            uri = url,
            title = title.ifBlank { url.substringAfterLast('/').ifBlank { "Stream" } },
            path = url,
            folderId = "network",
            folderName = "Network",
            durationMs = 0L,
            sizeBytes = 0L,
            width = 0,
            height = 0,
            dateAdded = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis(),
            mimeType = "video/*",
            bitrate = 0,
            isNetwork = true,
        )
        db.videos().upsert(entity)
        return entity
    }

    suspend fun createPlaylist(name: String): Long =
        db.playlists().insert(
            PlaylistEntity(
                name = name.trim().ifEmpty { "Untitled playlist" },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )

    suspend fun renamePlaylist(id: Long, name: String) {
        val p = db.playlists().get(id) ?: return
        db.playlists().update(p.copy(name = name.trim().ifEmpty { p.name }, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePlaylist(id: Long) {
        db.playlists().clearItems(id)
        db.playlists().delete(id)
    }

    suspend fun addToPlaylist(playlistId: Long, videoId: String) {
        val items = db.playlists().getItems(playlistId)
        if (items.any { it.videoId == videoId }) return
        db.playlists().upsertItem(
            com.aether.player.data.db.PlaylistItemEntity(playlistId, videoId, items.size),
        )
        db.playlists().get(playlistId)?.let {
            db.playlists().update(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun removeFromPlaylist(playlistId: Long, videoId: String) =
        db.playlists().removeItem(playlistId, videoId)

    suspend fun reorderPlaylist(playlistId: Long, videoIds: List<String>) =
        db.playlists().replaceItems(playlistId, videoIds)

    suspend fun addBookmark(videoId: String, positionMs: Long, title: String) {
        db.bookmarks().insert(
            BookmarkEntity(
                videoId = videoId,
                positionMs = positionMs,
                title = title,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun renameBookmark(id: Long, title: String) = db.bookmarks().rename(id, title)
    suspend fun deleteBookmark(id: Long) = db.bookmarks().delete(id)

    suspend fun saveUrl(url: String, title: String) {
        db.savedUrls().insert(
            SavedUrlEntity(url = url, title = title, lastOpenedAt = System.currentTimeMillis()),
        )
    }

    suspend fun deleteSavedUrl(id: Long) = db.savedUrls().delete(id)

    suspend fun enqueueDownload(url: String, title: String): Long =
        db.downloads().insert(
            DownloadEntity(
                url = url,
                title = title,
                status = "queued",
                progress = 0,
                localUri = null,
                createdAt = System.currentTimeMillis(),
            ),
        )

    suspend fun updateDownload(id: Long, status: String, progress: Int, localUri: String?) =
        db.downloads().update(id, status, progress, localUri)

    suspend fun deleteDownload(id: Long) = db.downloads().delete(id)

    suspend fun clearHistory() = db.history().clear()
    suspend fun deleteHistory(id: Long) = db.history().delete(id)

    suspend fun deleteVideoRecord(id: String) = db.videos().delete(id)

    fun sorted(videos: Flow<List<VideoEntity>>, sort: Flow<SortMode>): Flow<List<VideoEntity>> =
        combine(videos, sort) { list, mode ->
            when (mode) {
                SortMode.NAME -> list.sortedBy { it.title.lowercase() }
                SortMode.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
                SortMode.DATE_MODIFIED -> list.sortedByDescending { it.dateModified }
                SortMode.DURATION -> list.sortedByDescending { it.durationMs }
                SortMode.SIZE -> list.sortedByDescending { it.sizeBytes }
                SortMode.LAST_PLAYED -> list.sortedByDescending { it.lastPlayedAt }
            }
        }

    suspend fun scanTree(treeUri: Uri): Pair<Int, Int> {
        val (videos, folders) = scanner.scanTree(treeUri)
        if (videos.isEmpty()) return 0 to 0
        val existing = db.videos().getAll().associateBy { it.id }
        val merged = videos.map { fresh ->
            val old = existing[fresh.id]
            if (old == null) fresh else fresh.copy(
                isFavorite = old.isFavorite,
                isHidden = old.isHidden,
                playCount = old.playCount,
                lastPlayedAt = old.lastPlayedAt,
                lastPositionMs = old.lastPositionMs,
                completed = old.completed,
            )
        }
        db.videos().upsertAll(merged)
        db.folders().upsertAll(folders)
        return merged.size to folders.size
    }

    suspend fun upsertLocalUri(uri: String, title: String, folderName: String = "Imports"): VideoEntity {
        db.videos().getByUri(uri)?.let { return it }
        val now = System.currentTimeMillis()
        val entity = VideoEntity(
            id = "saf-${uri.hashCode()}-$now",
            uri = uri,
            title = title.ifBlank { uri.substringAfterLast('/').ifBlank { "Video" } },
            path = uri,
            folderId = "saf-imports",
            folderName = folderName,
            durationMs = 0L,
            sizeBytes = 0L,
            width = 0,
            height = 0,
            dateAdded = now,
            dateModified = now,
            mimeType = "video/*",
            bitrate = 0,
            isNetwork = false,
        )
        db.videos().upsert(entity)
        return entity
    }

    suspend fun setVideoTitle(id: String, title: String) = db.videos().setTitle(id, title)

    suspend fun updateDownloadSystemId(id: Long, systemId: Long) = db.downloads().setSystemId(id, systemId)

    suspend fun getDownload(id: Long) = db.downloads().get(id)

    suspend fun movePlaylistItem(playlistId: Long, from: Int, to: Int) {
        val items = db.playlists().getItems(playlistId)
        if (from !in items.indices || to !in items.indices || from == to) return
        val ids = items.map { it.videoId }.toMutableList()
        val moved = ids.removeAt(from)
        ids.add(to, moved)
        db.playlists().replaceItems(playlistId, ids)
    }
}
