package com.aether.player.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.aether.player.ui.components.GlassSettingsRow
import com.aether.player.ui.components.SectionHeader
import com.aether.player.ui.components.VideoListRow
import com.aether.player.ui.library.LibraryViewModel
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    vm: LibraryViewModel,
    onBack: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
) {
    val ui by vm.ui.collectAsState()
    var text by remember { mutableStateOf(ui.query) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(text) {
        delay(180)
        vm.setQuery(text)
    }
    val q = text.trim().lowercase()
    val folders = if (q.isBlank()) emptyList() else ui.folders.filter { it.name.lowercase().contains(q) }
    val playlists = if (q.isBlank()) emptyList() else ui.playlists.filter { it.name.lowercase().contains(q) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        TextButton(onClick = {
            vm.setQuery("")
            onBack()
        }) { Text("← Back") }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            placeholder = { Text("Search videos, folders, playlists") },
            singleLine = true,
        )
        LazyColumn(contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)) {
            if (folders.isNotEmpty()) {
                item {
                    SectionHeader("Folders")
                    folders.forEach { folder ->
                        GlassSettingsRow(
                            title = folder.name,
                            subtitle = "${folder.videoCount} videos",
                            onClick = { onOpenFolder(folder.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            if (playlists.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Playlists")
                    playlists.forEach { playlist ->
                        GlassSettingsRow(
                            title = playlist.name,
                            onClick = { onOpenPlaylist(playlist.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            if (ui.videos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Videos")
                }
            }
            items(ui.videos, key = { it.id }) { video ->
                VideoListRow(
                    video,
                    onClick = {
                        vm.play(video, ui.videos)
                        onOpenVideo()
                    },
                    onLongClick = { vm.toggleFavorite(video.id) },
                )
            }
            if (q.isNotBlank() && folders.isEmpty() && playlists.isEmpty() && ui.videos.isEmpty()) {
                item {
                    Text(
                        "No results for \"$text\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
        }
    }
}
