package com.aether.player.ui.library

import android.app.Application
import android.app.DownloadManager
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.player.AetherApp
import com.aether.player.data.db.DownloadEntity
import com.aether.player.data.db.FolderEntity
import com.aether.player.data.db.PlaylistEntity
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.library.FileResult
import com.aether.player.data.prefs.AetherSettings
import com.aether.player.data.prefs.SortMode
import com.aether.player.data.prefs.ViewMode
import com.aether.player.domain.UrlValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFilter { ALL, FAVORITE, UNWATCHED, PARTIAL, RECENT, HIDDEN }
enum class ResFilter { ALL, HD, FULL_HD, UHD }

sealed interface PendingOp {
    data class Rename(val id: String, val uri: String, val name: String) : PendingOp
    data class Delete(val id: String, val uri: String) : PendingOp
}

data class PendingConsent(val sender: IntentSender, val op: PendingOp)

data class HistoryItem(
    val id: Long,
    val video: VideoEntity?,
    val playedAt: Long,
    val positionMs: Long,
    val durationMs: Long,
)

data class LibraryUi(
    val videos: List<VideoEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val continueWatching: List<VideoEntity> = emptyList(),
    val recentlyAdded: List<VideoEntity> = emptyList(),
    val recentlyPlayed: List<VideoEntity> = emptyList(),
    val favorites: List<VideoEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val scanning: Boolean = false,
    val scanError: String? = null,
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val resFilter: ResFilter = ResFilter.ALL,
    val sort: SortMode = SortMode.DATE_ADDED,
    val viewMode: ViewMode = ViewMode.GRID,
    val settings: AetherSettings = AetherSettings(),
    val count: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AetherApp
    private val library = app.container.library
    private val prefs = app.container.preferences
    private val mediaOps = app.container.mediaOps
    private val player = app.container.playerManager

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val resFilter = MutableStateFlow(ResFilter.ALL)
    private val folderId = MutableStateFlow<String?>(null)

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    private val _pendingConsent = MutableStateFlow<PendingConsent?>(null)
    val pendingConsent: StateFlow<PendingConsent?> = _pendingConsent

    val ui: StateFlow<LibraryUi> = combine(
        combine(
            combine(query, filter) { q, f -> q to f }.flatMapLatest { (q, f) ->
                if (f == LibraryFilter.HIDDEN && q.isBlank()) library.hiddenVideos()
                else if (q.isBlank()) library.videos()
                else library.search(q.trim())
            },
            library.folders(),
            library.continueWatching(),
            library.recentlyAdded(),
            library.recent(),
        ) { videos, folders, cont, added, recent ->
            listOf(videos, folders, cont, added, recent)
        },
        combine(
            library.favorites(),
            library.playlists(),
            library.scanning,
            library.scanError,
            query,
        ) { fav, playlists, scanning, scanError, q ->
            listOf(fav, playlists, scanning, scanError, q)
        },
        combine(
            filter,
            resFilter,
            prefs.settings,
            library.videoCount(),
        ) { f, res, settings, count ->
            listOf(f, res, settings, count)
        },
    ) { packA, packB, packC ->
        @Suppress("UNCHECKED_CAST")
        val videos = packA[0] as List<VideoEntity>
        val folders = packA[1] as List<FolderEntity>
        val cont = packA[2] as List<VideoEntity>
        val added = packA[3] as List<VideoEntity>
        val recent = packA[4] as List<VideoEntity>
        val fav = packB[0] as List<VideoEntity>
        val playlists = packB[1] as List<PlaylistEntity>
        val scanning = packB[2] as Boolean
        val scanError = packB[3] as String?
        val q = packB[4] as String
        val f = packC[0] as LibraryFilter
        val res = packC[1] as ResFilter
        val settings = packC[2] as AetherSettings
        val count = packC[3] as Int
        val sorted = sort(videos, settings.sortMode)
        LibraryUi(
            videos = applyResFilter(applyFilter(sorted, f), res),
            folders = folders,
            continueWatching = cont,
            recentlyAdded = added,
            recentlyPlayed = recent,
            favorites = fav,
            playlists = playlists,
            scanning = scanning,
            scanError = scanError,
            query = q,
            filter = f,
            resFilter = res,
            sort = settings.sortMode,
            viewMode = settings.viewMode,
            settings = settings,
            count = count,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUi())

    val folderVideos: StateFlow<List<VideoEntity>> = folderId.flatMapLatest { id ->
        if (id == null) library.videos() else library.folderVideos(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyItems: StateFlow<List<HistoryItem>> =
        combine(library.history(), library.videos()) { hist, videos ->
            val map = videos.associateBy { it.id }
            hist.map { HistoryItem(it.id, map[it.videoId], it.playedAt, it.positionMs, it.durationMs) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = library.downloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun notify(msg: String) {
        _notice.value = msg
    }

    fun consumeNotice() {
        _notice.value = null
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: LibraryFilter) {
        filter.value = value
    }

    fun setResFilter(value: ResFilter) {
        resFilter.value = value
    }

    fun openFolder(id: String?) {
        folderId.value = id
    }

    fun scan() {
        viewModelScope.launch {
            val s = prefs.settings.first()
            val excluded = s.excludedFolders
                .split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            library.scanLibrary(excluded, s.hideHiddenFiles)
        }
    }

    fun play(video: VideoEntity, queue: List<VideoEntity> = listOf(video)) {
        val index = queue.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        player.playQueue(if (queue.isEmpty()) listOf(video) else queue, index)
    }

    fun playNext(video: VideoEntity) = player.addToQueue(video, playNext = true)
    fun addToQueue(video: VideoEntity) = player.addToQueue(video, playNext = false)

    fun playUrl(raw: String, title: String, onPlay: () -> Unit = {}) {
        val normalized = if (raw.contains("://")) raw.trim() else "https://${raw.trim()}"
        if (!UrlValidator.isPlayableUrl(normalized)) {
            notify("That doesn't look like a playable URL")
            return
        }
        if (UrlValidator.isWebPage(normalized)) {
            notify("That looks like a web page — paste a direct .mp4 / .m3u8 / .mpd link instead")
            return
        }
        viewModelScope.launch {
            val video = library.upsertNetworkVideo(normalized, title.ifBlank { normalized })
            library.saveUrl(normalized, video.title)
            player.playSingle(video, startOver = true)
            onPlay()
        }
    }

    fun openVideoDoc(uri: Uri, onPlay: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val name = queryDisplayName(uri) ?: uri.lastPathSegment ?: "Video"
            val video = library.upsertLocalUri(uri.toString(), name)
            player.playSingle(video, startOver = true)
            onPlay()
        }
    }

    fun importFolder(treeUri: Uri) {
        viewModelScope.launch {
            val (videos, folders) = library.scanTree(treeUri)
            notify(
                if (videos == 0) "No videos found in that folder"
                else "Imported $videos video(s) from $folders folder(s)",
            )
        }
    }

    fun toggleFavorite(id: String) = viewModelScope.launch { library.toggleFavorite(id) }

    fun hide(id: String) = viewModelScope.launch { library.setHidden(id, true) }

    fun unhide(id: String) = viewModelScope.launch { library.setHidden(id, false) }

    fun markWatched(id: String, watched: Boolean) =
        viewModelScope.launch {
            library.markWatched(id, watched)
            notify(if (watched) "Marked watched" else "Marked unwatched")
        }

    fun renameVideo(id: String, name: String) {
        viewModelScope.launch {
            val video = library.getVideo(id) ?: return@launch
            when (val r = mediaOps.rename(video.uri, name)) {
                FileResult.Done -> {
                    library.setVideoTitle(id, name.trim())
                    notify("Renamed")
                }
                is FileResult.Consent -> _pendingConsent.value =
                    PendingConsent(r.sender, PendingOp.Rename(id, video.uri, name))
                is FileResult.Error -> notify(r.message)
            }
        }
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch {
            val video = library.getVideo(id) ?: return@launch
            if (video.isNetwork || video.uri.startsWith("http")) {
                library.deleteVideoRecord(id)
                notify("Removed")
                return@launch
            }
            when (val r = mediaOps.delete(video.uri)) {
                FileResult.Done -> {
                    library.deleteVideoRecord(id)
                    notify("Deleted")
                }
                is FileResult.Consent -> _pendingConsent.value =
                    PendingConsent(r.sender, PendingOp.Delete(id, video.uri))
                is FileResult.Error -> notify(r.message)
            }
        }
    }

    fun retryPending() {
        val pending = _pendingConsent.value ?: return
        _pendingConsent.value = null
        when (val op = pending.op) {
            is PendingOp.Rename -> renameVideo(op.id, op.name)
            is PendingOp.Delete -> deleteVideo(op.id)
        }
    }

    fun clearPending() {
        _pendingConsent.value = null
    }

    fun copyOrMove(id: String, treeUri: Uri, move: Boolean) {
        viewModelScope.launch {
            val video = library.getVideo(id) ?: return@launch
            notify(if (move) "Moving…" else "Copying…")
            val dest = mediaOps.copyToTree(video.uri, treeUri, video.title)
            if (dest == null) {
                notify("Copy failed")
                return@launch
            }
            library.upsertLocalUri(dest, video.title, folderName = "Copies")
            if (move) {
                when (val r = mediaOps.delete(video.uri)) {
                    FileResult.Done -> library.deleteVideoRecord(id)
                    is FileResult.Consent -> _pendingConsent.value =
                        PendingConsent(r.sender, PendingOp.Delete(id, video.uri))
                    is FileResult.Error -> notify("Copied, but the original could not be removed")
                }
            }
            notify(if (move) "Moved" else "Copied")
        }
    }

    fun createPlaylist(name: String, videoId: String? = null) = viewModelScope.launch {
        val id = library.createPlaylist(name)
        if (videoId != null) library.addToPlaylist(id, videoId)
        notify("Playlist created")
    }

    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch {
        library.renamePlaylist(id, name)
        notify("Playlist renamed")
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch {
        library.deletePlaylist(id)
        notify("Playlist deleted")
    }

    fun addToPlaylist(playlistId: Long, videoId: String) =
        viewModelScope.launch { library.addToPlaylist(playlistId, videoId) }

    fun removeFromPlaylist(playlistId: Long, videoId: String) =
        viewModelScope.launch { library.removeFromPlaylist(playlistId, videoId) }

    fun movePlaylistItem(playlistId: Long, from: Int, to: Int) =
        viewModelScope.launch { library.movePlaylistItem(playlistId, from, to) }

    fun deleteSavedUrl(id: Long) = viewModelScope.launch { library.deleteSavedUrl(id) }

    fun clearHistory() = viewModelScope.launch { library.clearHistory(); notify("History cleared") }
    fun deleteHistory(id: Long) = viewModelScope.launch { library.deleteHistory(id) }

    fun deleteDownload(id: Long) = viewModelScope.launch { library.deleteDownload(id) }

    fun refreshDownload(id: Long) {
        viewModelScope.launch {
            val row = library.getDownload(id) ?: return@launch
            if (row.systemDownloadId < 0) {
                notify("Unknown system download")
                return@launch
            }
            val dm = getApplication<Application>().getSystemService(DownloadManager::class.java)
            val cursor = dm.query(DownloadManager.Query().setFilterById(row.systemDownloadId))
            cursor?.use { c ->
                if (!c.moveToFirst()) {
                    library.updateDownload(id, "error", row.progress, row.localUri)
                    return@use
                }
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val local = runCatching {
                            dm.getUriForDownloadedFile(row.systemDownloadId)?.toString()
                        }.getOrNull()
                        library.updateDownload(id, "done", 100, local)
                        if (local != null) {
                            library.upsertLocalUri(local, row.title, folderName = "Downloads")
                        }
                    }
                    DownloadManager.STATUS_FAILED -> library.updateDownload(id, "error", 0, row.localUri)
                    else -> {
                        val pct = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else row.progress
                        library.updateDownload(id, "downloading", pct, row.localUri)
                    }
                }
            }
        }
    }

    fun setSort(mode: SortMode) = viewModelScope.launch { prefs.update { it.copy(sortMode = mode) } }
    fun setViewMode(mode: ViewMode) = viewModelScope.launch { prefs.update { it.copy(viewMode = mode) } }

    fun share(video: VideoEntity): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = video.mimeType ?: "video/*"
            putExtra(Intent.EXTRA_STREAM, video.uri.toUri())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun openWith(video: VideoEntity): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(video.uri.toUri(), video.mimeType ?: "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun enqueueDownload(url: String, title: String) {
        viewModelScope.launch {
            val name = title.ifBlank {
                url.substringAfterLast('/').substringBefore('?').ifBlank { "aether-download" }
            }
            val id = library.enqueueDownload(url, name)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(name)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Aether/$name")
                .setAllowedOverMetered(true)
            val dm = getApplication<Application>().getSystemService(DownloadManager::class.java)
            runCatching { dm.enqueue(request) }
                .onSuccess { sysId ->
                    library.updateDownloadSystemId(id, sysId)
                    library.updateDownload(id, "downloading", 0, null)
                    notify("Download started")
                }
                .onFailure {
                    library.updateDownload(id, "error", 0, it.message)
                    notify("Download failed to start")
                }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        getApplication<Application>().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    private fun sort(list: List<VideoEntity>, mode: SortMode): List<VideoEntity> = when (mode) {
        SortMode.NAME -> list.sortedBy { it.title.lowercase() }
        SortMode.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
        SortMode.DATE_MODIFIED -> list.sortedByDescending { it.dateModified }
        SortMode.DURATION -> list.sortedByDescending { it.durationMs }
        SortMode.SIZE -> list.sortedByDescending { it.sizeBytes }
        SortMode.LAST_PLAYED -> list.sortedByDescending { it.lastPlayedAt }
    }

    private fun applyFilter(list: List<VideoEntity>, filter: LibraryFilter): List<VideoEntity> = when (filter) {
        LibraryFilter.ALL -> list
        LibraryFilter.FAVORITE -> list.filter { it.isFavorite }
        LibraryFilter.UNWATCHED -> list.filter { it.lastPlayedAt == 0L }
        LibraryFilter.PARTIAL -> list.filter { it.lastPositionMs > 4_000L && !it.completed }
        LibraryFilter.RECENT -> list.sortedByDescending { it.lastPlayedAt }.take(60)
        LibraryFilter.HIDDEN -> list
    }

    private fun applyResFilter(list: List<VideoEntity>, res: ResFilter): List<VideoEntity> = when (res) {
        ResFilter.ALL -> list
        ResFilter.HD -> list.filter { it.height >= 720 }
        ResFilter.FULL_HD -> list.filter { it.height >= 1080 }
        ResFilter.UHD -> list.filter { it.height >= 2160 || it.width >= 3840 }
    }
}
