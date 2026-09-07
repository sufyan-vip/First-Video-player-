package com.aether.player.ui.library

import android.app.Application
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.player.AetherApp
import com.aether.player.data.db.FolderEntity
import com.aether.player.data.db.PlaylistEntity
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.prefs.AetherSettings
import com.aether.player.data.prefs.SortMode
import com.aether.player.data.prefs.ViewMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFilter { ALL, FAVORITE, UNWATCHED, PARTIAL, RECENT }

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
    private val player = app.container.playerManager

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val folderId = MutableStateFlow<String?>(null)

    val ui: StateFlow<LibraryUi> = combine(
        combine(
            query.flatMapLatest { q ->
                if (q.isBlank()) library.videos() else library.search(q.trim())
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
        combine(filter, prefs.settings, library.videoCount()) { f, settings, count ->
            Triple(f, settings, count)
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
        val f = packC.first
        val settings = packC.second
        val count = packC.third
        val sorted = sort(videos, settings.sortMode)
        LibraryUi(
            videos = applyFilter(sorted, f),
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
            sort = settings.sortMode,
            viewMode = settings.viewMode,
            settings = settings,
            count = count,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUi())

    val folderVideos: StateFlow<List<VideoEntity>> = folderId.flatMapLatest { id ->
        if (id == null) library.videos() else library.folderVideos(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: LibraryFilter) {
        filter.value = value
    }

    fun openFolder(id: String?) {
        folderId.value = id
    }

    fun scan() {
        viewModelScope.launch {
            val excluded = prefs.settings.stateIn(viewModelScope).value.excludedFolders
                .split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            library.scanLibrary(excluded)
        }
    }

    fun play(video: VideoEntity, queue: List<VideoEntity> = listOf(video)) {
        val index = queue.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        player.playQueue(if (queue.isEmpty()) listOf(video) else queue, index)
    }

    fun playNext(video: VideoEntity) = player.addToQueue(video, playNext = true)
    fun addToQueue(video: VideoEntity) = player.addToQueue(video, playNext = false)

    fun toggleFavorite(id: String) = viewModelScope.launch { library.toggleFavorite(id) }

    fun hide(id: String) = viewModelScope.launch { library.setHidden(id, true) }

    fun createPlaylist(name: String, videoId: String? = null) = viewModelScope.launch {
        val id = library.createPlaylist(name)
        if (videoId != null) library.addToPlaylist(id, videoId)
    }

    fun addToPlaylist(playlistId: Long, videoId: String) =
        viewModelScope.launch { library.addToPlaylist(playlistId, videoId) }

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
            val id = library.enqueueDownload(url, title)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, title.ifBlank { "aether-download" })
                .setAllowedOverMetered(true)
            val dm = getApplication<Application>().getSystemService(DownloadManager::class.java)
            runCatching {
                dm.enqueue(request)
                library.updateDownload(id, "downloading", 0, null)
            }.onFailure {
                library.updateDownload(id, "error", 0, it.message)
            }
        }
    }

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
    }
}
