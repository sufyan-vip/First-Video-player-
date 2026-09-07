package com.aether.player.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aether.player.AetherApp
import com.aether.player.data.db.VideoEntity
import com.aether.player.ui.components.ContinueCard
import com.aether.player.ui.components.FolderCard
import com.aether.player.ui.components.GlassButton
import com.aether.player.ui.components.GlassCard
import com.aether.player.ui.components.GlassIconButton
import com.aether.player.ui.components.SectionHeader
import com.aether.player.ui.components.VideoGridCard
import com.aether.player.ui.library.LibraryViewModel

@Composable
fun HomeScreen(
    vm: LibraryViewModel,
    onOpenVideo: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUrl: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as AetherApp
    val savedUrls by app.container.library.savedUrls().collectAsState(initial = emptyList())

    val notice by vm.notice.collectAsState()
    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeNotice()
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.openVideoDoc(uri) { onOpenVideo() }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.importFolder(uri)
    }

    fun play(v: VideoEntity, queue: List<VideoEntity> = ui.videos) {
        vm.play(v, queue)
        onOpenVideo()
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 96.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Aether", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${ui.count} videos in your library",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row {
                GlassIconButton(Icons.Outlined.Search, "Search", onClick = onOpenSearch)
                Spacer(Modifier.width(8.dp))
                GlassIconButton(Icons.Outlined.Settings, "Settings", onClick = onOpenSettings)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("Open video", icon = Icons.Outlined.VideoFile, filled = true, onClick = {
                videoPicker.launch(arrayOf("video/*"))
            })
            GlassButton("Open folder", icon = Icons.Outlined.FolderOpen, onClick = { folderPicker.launch(null) })
            GlassButton("Open URL", icon = Icons.Outlined.Link, onClick = onOpenUrl)
            GlassButton("Scan", icon = Icons.Outlined.VideoFile, onClick = { vm.scan() })
            GlassButton("Folders", icon = Icons.Outlined.FolderOpen, onClick = onOpenFolder)
            GlassButton("Playlists", icon = Icons.Outlined.PlaylistPlay, onClick = onOpenPlaylists)
            GlassButton("History", icon = Icons.Outlined.History, onClick = onOpenHistory)
            GlassButton("Downloads", icon = Icons.Outlined.Download, onClick = onOpenDownloads)
        }
        if (ui.scanning) {
            Spacer(Modifier.height(12.dp))
            Text("Scanning library…", color = MaterialTheme.colorScheme.primary)
        }
        ui.scanError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (ui.continueWatching.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Continue watching")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.continueWatching.forEach { v ->
                    ContinueCard(v, onClick = { play(v, ui.continueWatching) })
                }
            }
        }
        if (ui.recentlyPlayed.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Recently played")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.recentlyPlayed.take(12).forEach { v ->
                    VideoGridCard(
                        video = v,
                        modifier = Modifier.width(160.dp),
                        onClick = { play(v, ui.recentlyPlayed) },
                        onLongClick = { vm.toggleFavorite(v.id) },
                    )
                }
            }
        }
        if (ui.recentlyAdded.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Recently added")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.recentlyAdded.take(12).forEach { v ->
                    VideoGridCard(
                        video = v,
                        modifier = Modifier.width(160.dp),
                        onClick = { play(v, ui.recentlyAdded) },
                        onLongClick = { vm.toggleFavorite(v.id) },
                    )
                }
            }
        }
        if (ui.favorites.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Favorites")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.favorites.take(12).forEach { v ->
                    VideoGridCard(
                        video = v,
                        modifier = Modifier.width(160.dp),
                        onClick = { play(v, ui.favorites) },
                        onLongClick = { vm.toggleFavorite(v.id) },
                    )
                }
            }
        }
        if (savedUrls.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Network sources", action = "Open URL", onAction = onOpenUrl)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                savedUrls.take(10).forEach { item ->
                    GlassCard(
                        modifier = Modifier.width(220.dp),
                        onClick = { vm.playUrl(item.url, item.title) { onOpenVideo() } },
                    ) {
                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text(
                            item.url,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (ui.folders.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Folders", action = "See all", onAction = onOpenFolder)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.folders.take(10).forEach { folder ->
                    FolderCard(folder, onClick = onOpenFolder, modifier = Modifier.width(160.dp))
                }
            }
        }
        if (ui.playlists.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Playlists", action = "Open", onAction = onOpenPlaylists)
            ui.playlists.take(5).forEach {
                Text(
                    it.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
        if (ui.count == 0 && !ui.scanning) {
            Spacer(Modifier.height(40.dp))
            Text("No videos yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Scan your library, open a file or folder, or open a URL to start playing.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
