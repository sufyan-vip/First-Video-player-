package com.aether.player.ui.library

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.player.data.db.VideoEntity
import com.aether.player.data.prefs.SortMode
import com.aether.player.data.prefs.ViewMode
import com.aether.player.domain.TimeFormat
import com.aether.player.ui.components.GlassChip
import com.aether.player.ui.components.GlassIconButton
import com.aether.player.ui.components.VideoGridCard
import com.aether.player.ui.components.VideoListRow

@Composable
fun VideosScreen(
    vm: LibraryViewModel,
    onOpenVideo: () -> Unit,
    onOpenSearch: () -> Unit,
    onShowInFolder: (String) -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var sortMenu by remember { mutableStateOf(false) }
    var actionId by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<VideoEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<VideoEntity?>(null) }
    var detailsOf by remember { mutableStateOf<VideoEntity?>(null) }
    var playlistTarget by remember { mutableStateOf<VideoEntity?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var treeTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val notice by vm.notice.collectAsState()
    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeNotice()
        }
    }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.retryPending() else vm.clearPending()
    }
    val pending by vm.pendingConsent.collectAsState()
    LaunchedEffect(pending) {
        pending?.let { consentLauncher.launch(IntentSenderRequest.Builder(it.sender).build()) }
    }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val target = treeTarget
        if (uri != null && target != null) {
            vm.copyOrMove(target.first, uri, target.second)
        }
        treeTarget = null
    }

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val maxW = maxWidth
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Videos", style = MaterialTheme.typography.headlineMedium)
                    Text("${ui.videos.size} items", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(Icons.Outlined.Refresh, "Rescan library", onClick = { vm.scan() })
                    GlassIconButton(Icons.Outlined.Search, "Search", onClick = onOpenSearch)
                    GlassIconButton(
                        if (ui.viewMode == ViewMode.LIST) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                        "View mode",
                        onClick = {
                            vm.setViewMode(if (ui.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                        },
                    )
                }
            }
            if (ui.scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
            ui.scanError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryFilter.entries.forEach { f ->
                    GlassChip(f.name.lowercase().replaceFirstChar { it.titlecase() }, ui.filter == f) { vm.setFilter(f) }
                }
                GlassChip("Sort", false) { sortMenu = true }
                GlassChip("Play all", false) {
                    ui.videos.firstOrNull()?.let {
                        vm.play(it, ui.videos)
                        onOpenVideo()
                    }
                }
                GlassChip("Shuffle", false) {
                    val shuffled = ui.videos.shuffled()
                    shuffled.firstOrNull()?.let {
                        vm.play(it, shuffled)
                        onOpenVideo()
                    }
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResFilter.entries.forEach { r ->
                    val label = when (r) {
                        ResFilter.ALL -> "Any quality"
                        ResFilter.HD -> "720p+"
                        ResFilter.FULL_HD -> "1080p+"
                        ResFilter.UHD -> "4K"
                    }
                    GlassChip(label, ui.resFilter == r) { vm.setResFilter(r) }
                }
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                SortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name.lowercase().replace('_', ' ')) },
                        onClick = {
                            vm.setSort(mode)
                            sortMenu = false
                        },
                    )
                }
            }
            if (ui.viewMode == ViewMode.LIST) {
                LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp)) {
                    items(ui.videos, key = { it.id }) { video ->
                        VideoListRow(
                            video,
                            onClick = {
                                vm.play(video, ui.videos)
                                onOpenVideo()
                            },
                            onLongClick = { actionId = video.id },
                        )
                    }
                }
            } else {
                val base = ui.settings.gridSize.coerceIn(2, 4)
                val adaptive = if (maxW < 600.dp) base else (maxW / 180.dp).toInt().coerceIn(3, 6)
                val cols = (if (ui.settings.compactMode) adaptive + 1 else adaptive).coerceIn(2, 6)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (ui.viewMode == ViewMode.COMPACT) (cols + 1).coerceAtMost(6) else cols),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(ui.videos, key = { it.id }) { video ->
                        VideoGridCard(
                            video,
                            onClick = {
                                vm.play(video, ui.videos)
                                onOpenVideo()
                            },
                            onLongClick = { actionId = video.id },
                        )
                    }
                }
            }
            DropdownMenu(expanded = actionId != null, onDismissRequest = { actionId = null }) {
                val video = ui.videos.find { it.id == actionId }
                DropdownMenuItem(
                    text = { Text(if (video?.isFavorite == true) "Unfavorite" else "Favorite") },
                    onClick = { video?.let { vm.toggleFavorite(it.id) }; actionId = null },
                )
                DropdownMenuItem(text = { Text("Play next") }, onClick = {
                    video?.let { vm.playNext(it) }
                    actionId = null
                })
                DropdownMenuItem(text = { Text("Add to queue") }, onClick = {
                    video?.let { vm.addToQueue(it) }
                    actionId = null
                })
                DropdownMenuItem(text = { Text("Add to playlist…") }, onClick = {
                    playlistTarget = video
                    newPlaylistName = ""
                    actionId = null
                })
                DropdownMenuItem(
                    text = { Text(if (video?.completed == true) "Mark unwatched" else "Mark watched") },
                    onClick = {
                        video?.let { vm.markWatched(it.id, !(it.completed)) }
                        actionId = null
                    },
                )
                DropdownMenuItem(text = { Text("Share") }, onClick = {
                    video?.let { runCatching { context.startActivity(Intent.createChooser(vm.share(it), "Share video")) } }
                    actionId = null
                })
                DropdownMenuItem(text = { Text("Open with…") }, onClick = {
                    video?.let { runCatching { context.startActivity(Intent.createChooser(vm.openWith(it), "Open with")) } }
                    actionId = null
                })
                if (video?.folderId != null) {
                    DropdownMenuItem(text = { Text("Show in folder") }, onClick = {
                        onShowInFolder(video.folderId!!)
                        actionId = null
                    })
                }
                DropdownMenuItem(text = { Text("Details") }, onClick = {
                    detailsOf = video
                    actionId = null
                })
                DropdownMenuItem(text = { Text("Rename") }, onClick = {
                    renaming = video
                    renameText = video?.title.orEmpty()
                    actionId = null
                })
                DropdownMenuItem(text = { Text("Copy to…") }, onClick = {
                    video?.let { treeTarget = it.id to false }
                    actionId = null
                    if (video != null) treePicker.launch(null)
                })
                DropdownMenuItem(text = { Text("Move to…") }, onClick = {
                    video?.let { treeTarget = it.id to true }
                    actionId = null
                    if (video != null) treePicker.launch(null)
                })
                DropdownMenuItem(
                    text = { Text(if (video?.isHidden == true) "Unhide" else "Hide") },
                    onClick = {
                        video?.let { if (it.isHidden) vm.unhide(it.id) else vm.hide(it.id) }
                        actionId = null
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        deleting = video
                        actionId = null
                    },
                )
            }
        }
    }

    renaming?.let { target ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename video") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameVideo(target.id, renameText)
                    renaming = null
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }
    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete video?") },
            text = { Text("\"${target.title}\" will be permanently deleted from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteVideo(target.id)
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
    detailsOf?.let { target ->
        AlertDialog(
            onDismissRequest = { detailsOf = null },
            title = { Text("Details") },
            text = {
                Text(
                    "Name: ${target.title}\n" +
                        "Folder: ${target.folderName ?: "—"}\n" +
                        "Size: ${TimeFormat.prettyBytes(target.sizeBytes)}\n" +
                        "Duration: ${TimeFormat.formatMs(target.durationMs)}\n" +
                        "Resolution: ${TimeFormat.prettyResolution(target.width, target.height)}\n" +
                        "Played: ${target.playCount} times\n" +
                        "Location: ${target.path ?: target.uri}",
                )
            },
            confirmButton = { TextButton(onClick = { detailsOf = null }) { Text("Close") } },
        )
    }
    playlistTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { playlistTarget = null },
            title = { Text("Add to playlist") },
            text = {
                Column {
                    if (ui.playlists.isEmpty()) {
                        Text("No playlists yet — create one below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ui.playlists.forEach { pl ->
                        TextButton(onClick = {
                            vm.addToPlaylist(pl.id, target.id)
                            playlistTarget = null
                        }) { Text(pl.name, maxLines = 1) }
                    }
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        singleLine = true,
                        placeholder = { Text("New playlist name") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newPlaylistName.isNotBlank(),
                    onClick = {
                        vm.createPlaylist(newPlaylistName.trim(), target.id)
                        playlistTarget = null
                    },
                ) { Text("Create & add") }
            },
            dismissButton = { TextButton(onClick = { playlistTarget = null }) { Text("Cancel") } },
        )
    }
}
