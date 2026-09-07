package com.aether.player.ui.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.aether.player.ui.components.FolderCard
import com.aether.player.ui.components.VideoListRow
import com.aether.player.ui.library.LibraryViewModel

@Composable
fun FoldersScreen(
    vm: LibraryViewModel,
    onOpenVideo: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val folderVideos by vm.folderVideos.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("Folders", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        if (selected == null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ui.folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder,
                        onClick = {
                            selected = folder.id
                            vm.openFolder(folder.id)
                        },
                    )
                }
            }
        } else {
            Text(
                ui.folders.find { it.id == selected }?.name ?: "Folder",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            androidx.compose.material3.TextButton(onClick = {
                selected = null
                vm.openFolder(null)
            }) { Text("← All folders") }
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(folderVideos, key = { it.id }) { video ->
                    VideoListRow(
                        video,
                        onClick = {
                            vm.play(video, folderVideos)
                            onOpenVideo()
                        },
                        onLongClick = { vm.toggleFavorite(video.id) },
                    )
                }
            }
        }
    }
}
