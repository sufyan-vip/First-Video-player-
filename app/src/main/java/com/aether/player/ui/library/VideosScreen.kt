package com.aether.player.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aether.player.data.prefs.SortMode
import com.aether.player.data.prefs.ViewMode
import com.aether.player.ui.components.GlassChip
import com.aether.player.ui.components.GlassIconButton
import com.aether.player.ui.components.VideoGridCard
import com.aether.player.ui.components.VideoListRow

@Composable
fun VideosScreen(
    vm: LibraryViewModel,
    onOpenVideo: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }
    var actionId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Videos", style = MaterialTheme.typography.headlineMedium)
                Text("${ui.videos.size} items", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryFilter.entries.forEach { f ->
                GlassChip(f.name.lowercase().replaceFirstChar { it.titlecase() }, ui.filter == f) { vm.setFilter(f) }
            }
            GlassChip("Sort", false) { sortMenu = true }
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
            val cols = ui.settings.gridSize.coerceIn(2, 4)
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (ui.viewMode == ViewMode.COMPACT) cols + 1 else cols),
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
            val id = actionId
            DropdownMenuItem(text = { Text("Favorite") }, onClick = { id?.let { vm.toggleFavorite(it) }; actionId = null })
            DropdownMenuItem(text = { Text("Play next") }, onClick = {
                id?.let { vid -> ui.videos.find { it.id == vid }?.let { vm.playNext(it) } }
                actionId = null
            })
            DropdownMenuItem(text = { Text("Add to queue") }, onClick = {
                id?.let { vid -> ui.videos.find { it.id == vid }?.let { vm.addToQueue(it) } }
                actionId = null
            })
            DropdownMenuItem(text = { Text("Hide") }, onClick = { id?.let { vm.hide(it) }; actionId = null })
        }
    }
}
